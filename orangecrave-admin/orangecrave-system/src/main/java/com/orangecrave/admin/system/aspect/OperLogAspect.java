package com.orangecrave.admin.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.log.OperLogContext;
import com.orangecrave.admin.system.entity.SysOperLog;
import com.orangecrave.admin.system.mapper.SysOperLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志切面：记录模块 类型/描述、方法、URL、参数、改前改后快照、操作人、IP、结果（见 01 篇 §1）。  */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final SysOperLogMapper operLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint pjp, OperLog operLog) throws Throwable {
        Throwable error = null;
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                save(pjp, operLog, error);
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }
            OperLogContext.clear();
        }
    }

    private void save(ProceedingJoinPoint pjp, OperLog operLog, Throwable error) {
        SysOperLog entity = new SysOperLog();
        entity.setOperModule(operLog.module());
        entity.setOperType(operLog.type());
        entity.setOperDesc(operLog.desc());

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        entity.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

        HttpServletRequest request = currentRequest();
        if (request != null) {
            entity.setRequestUrl(request.getRequestURI());
            entity.setOperIp(clientIp(request));
        }
        entity.setRequestParam(toJson(pjp.getArgs()));
        entity.setBeforeData(OperLogContext.getBefore());
        entity.setAfterData(OperLogContext.getAfter());

        if (StpUtil.isLogin()) {
            entity.setOperUserId(StpUtil.getLoginIdAsLong());
            Object nickname = StpUtil.getSession().get("nickname");
            entity.setOperUserName(nickname == null ? null : nickname.toString());
        }
        entity.setOperTime(LocalDateTime.now());
        entity.setStatus(error == null ? 1 : 0);
        entity.setErrorMsg(error == null ? null : truncate(error.getMessage()));
        operLogMapper.insert(entity);
    }

    private String toJson(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> serializable = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null || arg instanceof HttpServletRequest || arg instanceof jakarta.servlet.http.HttpServletResponse) {
                continue;
            }
            serializable.add(arg);
        }
        if (serializable.isEmpty()) {
            return null;
        }
        try {
            return truncate(objectMapper.writeValueAsString(serializable));
        } catch (Exception e) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > 2000 ? text.substring(0, 2000) : text;
    }
}
