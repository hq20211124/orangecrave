package com.orangecrave.admin.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 秒杀下单的受理回执（见 03 篇 §2.6）：只告诉客户端「已受理」和「拿什么去查结果」，
 * 不含订单号——订单此刻还没落库。
 */
@Data
@AllArgsConstructor
public class SeckillSubmitVO {

    private String requestId;
    /** 恒为 QUEUED；保留该字段是为了让回执与轮询结果用同一套状态词表。 */
    private String status;
}
