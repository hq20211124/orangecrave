package com.orangecrave.admin.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.orangecrave.admin.common.satoken.StpClientUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 登录校验。token 读取名与前缀在 application.yml 配置：
 * sa-token.token-name=Authorization, sa-token.token-prefix=Bearer（见 00 篇 §4.1）。
 * 后台与顾客端分属不同 Sa-Token 登录体系（loginType）。 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private static final String[] ADMIN_WHITE_LIST = {
            "/admin-api/auth/login",
            "/admin-api/health",
            "/admin-api/file/download",
            "/error"
    };

    private static final String[] CLIENT_WHITE_LIST = {
            "/client-api/auth/wx-login",
            "/client-api/channel/visit",
            "/client-api/file/download",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/admin-api/**")
                .excludePathPatterns(ADMIN_WHITE_LIST);

        registry.addInterceptor(new SaInterceptor(handle -> StpClientUtil.checkLogin()))
                .addPathPatterns("/client-api/**")
                .excludePathPatterns(CLIENT_WHITE_LIST);
    }
}
