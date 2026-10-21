package com.orangecrave.admin.common.limit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.common.result.ResultCode;
import com.orangecrave.admin.common.satoken.StpClientUtil;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 入口限流（见 03 篇 §2.6 削峰与限流）。只对 {@code orangecrave.rate-limit.rules} 里显式声明的路径生效，
 * 未声明的路径一律放行——限流是给已知高压入口用的，不对全站一刀切。
 *
 * <p>排在过滤器链最前（{@link Ordered#HIGHEST_PRECEDENCE}）：令牌不足时直接回信封拒绝，
 * 让请求在进入幂等去重、鉴权、业务之前就被挡掉，省下的正是最贵的那部分资源。
 * HTTP 状态保持 200、错误码放信封里（429），与全站响应契约一致。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** 子树规则后缀：以它结尾的键表示「该前缀下的所有路径」。 */
    private static final String SUBTREE_SUFFIX = "/**";

    private final RateLimiterFactory limiterFactory;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    /** 一条策略一个限流器实例；首次命中时按配置创建，避免每条请求都新建桶。 */
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimiterFactory limiterFactory, RateLimitProperties properties,
                           ObjectMapper objectMapper) {
        this.limiterFactory = limiterFactory;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled() || properties.getRules().isEmpty();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String rule = matchRule(request.getRequestURI());
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }
        RateLimiter limiter = limiters.computeIfAbsent(rule, r -> {
            RateLimitProperties.Rule config = properties.getRules().get(r);
            return limiterFactory.create(config.getPermitsPerSecond(), config.getBurst());
        });
        if (!limiter.tryAcquire(rule + ":" + identity(request))) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * 规则匹配：精确路径按等值命中，以 {@code /**} 结尾的按子树命中；同时候选取更长者，
     * 于是「宽的子树 + 单个精确收口」可以共存。精确匹配是默认语义，避免相邻路径（提交 vs 轮询）
     * 被同一条规则连带限住。
     */
    private String matchRule(String path) {
        String matched = null;
        for (String key : properties.getRules().keySet()) {
            boolean hit = key.endsWith(SUBTREE_SUFFIX)
                    ? path.startsWith(key.substring(0, key.length() - SUBTREE_SUFFIX.length()))
                    : path.equals(key);
            if (hit && (matched == null || key.length() > matched.length())) {
                matched = key;
            }
        }
        return matched;
    }

    /**
     * 限流维度：已登录按身份（同一 WiFi 下的不同用户互不牵连），未登录按来源 IP。
     * 读取登录态失败不构成拒绝理由，退回按 IP 计数。
     */
    private String identity(HttpServletRequest request) {
        try {
            Object adminId = StpUtil.getLoginIdDefaultNull();
            if (adminId != null) {
                return "a" + adminId;
            }
        } catch (Exception ignored) {
            // 后台令牌缺失/非法：正常，继续试顾客端
        }
        try {
            Object clientId = StpClientUtil.stpLogic.getLoginIdDefaultNull();
            if (clientId != null) {
                return "c" + clientId;
            }
        } catch (Exception ignored) {
            // 同上
        }
        return "ip" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(
                ResultCode.TOO_MANY_REQUESTS.getCode(), ResultCode.TOO_MANY_REQUESTS.getMsg())));
    }
}
