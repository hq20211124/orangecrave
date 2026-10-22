package com.orangecrave.admin.common.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头。防止点击劫持、MIME 嗅探、XSS 等常见 Web 攻击。
 * 对所有请求生效，优先级高于业务 Filter。
 */
@Component
@Order(1)
public class SecurityHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("X-Content-Type-Options", "nosniff");
        // 后台是 iframe 布局（layout.html 内嵌 pages/*），DENY 会让同源页面也拒绝渲染，
        // 故用 SAMEORIGIN：既能防外站点击劫持，又允许自身嵌套。
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        chain.doFilter(request, response);
    }
}
