package com.orangecrave.admin.system.config;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.common.result.ResultCode;
import com.orangecrave.admin.system.entity.SysIdempotency;
import com.orangecrave.admin.system.mapper.SysIdempotencyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

/**
 * 写操作幂等去重（见 02 篇 §1.5）：幂等键缺失直接拒绝；同键重复请求重放首次成功响应，不再执行业务。
 * 放在过滤器而不是拦截器，是因为要缓冲响应体才能重放。 */
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final int PROCESSING = 1;
    private static final int DONE = 2;
    private static final int FAILED = 3;
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final SysIdempotencyMapper idempotencyMapper;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (READ_METHODS.contains(request.getMethod().toUpperCase())) {
            return true;
        }
        if (!uri.startsWith("/admin-api/") && !uri.startsWith("/client-api/")) {
            return true;
        }
        // 登录时还没有令牌；上传是二进制 multipart 且只返回一个文件 key，重复上传不产生业务后果
        if (uri.startsWith("/admin-api/auth/login")
                || uri.startsWith("/client-api/auth/wx-login")
                || uri.contains("/file/upload")) {
            return true;
        }
        // 用 POST 承载的只读接口：重放旧结果会读到过期数据，不参与幂等
        return uri.endsWith("/preview") || uri.endsWith("/page") || uri.endsWith("/list")
                || uri.endsWith("/query") || uri.endsWith("/check") || uri.endsWith("/statistics")
                || uri.endsWith("/batch-export") || uri.endsWith("/todo");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank() || key.length() > 64) {
            reject(response, "写操作必须携带 Idempotency-Key 头");
            return;
        }
        String api = request.getMethod().toUpperCase() + " " + uri(request);
        SysIdempotency record = find(key, api);
        if (record == null) {
            record = newRecord(key, api);
            try {
                idempotencyMapper.insert(record);
            } catch (DuplicateKeyException e) {
                // 并发下另一路同号请求抢先落库
                reject(response, "重复请求正在处理中，请稍后再查结果");
                return;
            }
        } else if (!claim(record)) {
            // 已完成 → 重放；处理中 → 让调用方等待
            replayOrReject(record, response);
            return;
        }

        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(cachedRequest, cachedResponse);
        } catch (IOException | ServletException | RuntimeException e) {
            markFailed(record);
            throw e;
        }

        byte[] body = cachedResponse.getContentAsByteArray();
        if (cachedResponse.getStatus() == 200 && isSuccessEnvelope(body)) {
            record.setStatus(DONE);
            record.setRequestHash(sha256(new String(cachedRequest.getContentAsByteArray(), StandardCharsets.UTF_8)));
            record.setResponseBody(new String(body, StandardCharsets.UTF_8));
            idempotencyMapper.updateById(record);
        } else {
            // 业务失败（HTTP 200 + code != 0 或 5xx）留作可重试，否则换键之前都进不来
            markFailed(record);
        }
        cachedResponse.copyBodyToResponse();
    }

    private SysIdempotency find(String key, String api) {
        return idempotencyMapper.selectOne(new LambdaQueryWrapper<SysIdempotency>()
                .eq(SysIdempotency::getIdemKey, key)
                .eq(SysIdempotency::getApi, api)
                .orderByAsc(SysIdempotency::getId)
                .last("LIMIT 1"));
    }

    private SysIdempotency newRecord(String key, String api) {
        SysIdempotency record = new SysIdempotency();
        record.setIdemKey(key);
        record.setApi(api);
        record.setStatus(PROCESSING);
        return record;
    }

    /** 只接管失败过的记录；处理中与已完成的记录不在这里执行。 */
    private boolean claim(SysIdempotency record) {
        if (record.getStatus() == null || record.getStatus() != FAILED) {
            return false;
        }
        int rows = idempotencyMapper.update(null, new LambdaUpdateWrapper<SysIdempotency>()
                .eq(SysIdempotency::getId, record.getId())
                .eq(SysIdempotency::getStatus, FAILED)
                .set(SysIdempotency::getStatus, PROCESSING)
                .set(SysIdempotency::getResponseBody, null));
        if (rows == 1) {
            record.setStatus(PROCESSING);
            record.setResponseBody(null);
            return true;
        }
        return false;
    }

    private void markFailed(SysIdempotency record) {
        record.setStatus(FAILED);
        idempotencyMapper.updateById(record);
    }

    private void replayOrReject(SysIdempotency record, HttpServletResponse response) throws IOException {
        if (record.getStatus() != null && record.getStatus() == DONE && record.getResponseBody() != null) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(record.getResponseBody());
            return;
        }
        reject(response, "重复请求正在处理中，请稍后再查结果");
    }

    private void reject(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(ResultCode.BIZ_VALIDATION.getCode(), msg)));
    }

    private boolean isSuccessEnvelope(byte[] body) {
        if (body.length == 0) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            return node.has("code") && node.get("code").asInt(-1) == ResultCode.SUCCESS.getCode();
        } catch (IOException e) {
            return false;
        }
    }

    private String uri(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }
}
