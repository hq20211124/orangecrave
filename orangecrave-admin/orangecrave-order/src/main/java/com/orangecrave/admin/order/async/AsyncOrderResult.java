package com.orangecrave.admin.order.async;

import lombok.Data;

/**
 * 异步下单结果（见 03 篇 §2.6）。客户端凭 requestId 轮询，看到的不外乎这三种状态。
 * 状态机只前进：QUEUED → SUCCESS / FAILED。
 */
@Data
public class AsyncOrderResult {

    /** 已受理，仍在排队或处理中。 */
    public static final String QUEUED = "QUEUED";
    /** 下单成功，{@code orderId} 有效。 */
    public static final String SUCCESS = "SUCCESS";
    /** 下单失败，{@code message} 为可展示的原因（如「秒杀库存已抢完」）。 */
    public static final String FAILED = "FAILED";

    private String status;
    private Long orderId;
    private String message;

    public static AsyncOrderResult queued() {
        AsyncOrderResult r = new AsyncOrderResult();
        r.status = QUEUED;
        return r;
    }

    public static AsyncOrderResult success(Long orderId) {
        AsyncOrderResult r = new AsyncOrderResult();
        r.status = SUCCESS;
        r.orderId = orderId;
        return r;
    }

    public static AsyncOrderResult failed(String message) {
        AsyncOrderResult r = new AsyncOrderResult();
        r.status = FAILED;
        r.message = message;
        return r;
    }
}
