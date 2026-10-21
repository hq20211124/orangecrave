package com.orangecrave.admin.common.satoken;

import cn.dev33.satoken.stp.StpLogic;

/**
 * 顾客端独立登录体系（Sa-Token 多账号，loginType = "client"）。
 * 后台用 StpUtil，顾客端用本工具，二者 token 互不干扰。 */
public final class StpClientUtil {

    public static final String TYPE = "client";
    public static final StpLogic stpLogic = new StpLogic(TYPE);

    private StpClientUtil() {
    }

    public static void login(Object id) {
        stpLogic.login(id);
    }

    public static void logout() {
        stpLogic.logout();
    }

    public static boolean isLogin() {
        return stpLogic.isLogin();
    }

    public static void checkLogin() {
        stpLogic.checkLogin();
    }

    public static long getLoginIdAsLong() {
        return stpLogic.getLoginIdAsLong();
    }

    public static String getTokenValue() {
        return stpLogic.getTokenValue();
    }

    public static String getTokenName() {
        return stpLogic.getTokenName();
    }
}
