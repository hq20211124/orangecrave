package com.orangecrave.admin.common.result;

/**
 * 统一错误码（见 02 篇 §1.2 / §3）
 */
public enum ResultCode {

    SUCCESS(0, "success"),
    UNAUTHORIZED(401, "未授权或登录已过期"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    /** 限流拒绝：信封 code 取 429 与 HTTP 语义对齐，HTTP 状态仍是 200（见 02 篇 §1.2） */
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    SERVER_ERROR(500, "服务端异常"),

    /** 保留：客户端用 1000 表示刷新 token，后端用 1 不占用该值 */
    REFRESH_TOKEN(1000, "刷新 token"),

    BIZ_VALIDATION(1001, "业务校验失败"),

    ORDER_ERROR(2001, "订单错误"),
    AFTERSALE_WINDOW_CLOSED(2101, "超出售后窗口"),
    GROUP_ERROR(2201, "拼团错误"),
    AUDIT_ERROR(3001, "审核错误"),
    GOODS_ERROR(4001, "商品错误"),
    SHARE_CODE_ERROR(5001, "分享码错误");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
