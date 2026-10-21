package com.orangecrave.admin.order.adapter;

import java.math.BigDecimal;

/**
 * 营销让利端口（见 04 篇 §4 优惠计算顺序）。
 * order 不能依赖 marketing（依赖方向是 marketing → order），故在此定端口、由 marketing 实现。
 * 顺序固定：活动价覆盖原价 → 优惠券抵扣 → 满额包邮判定 → 运费模板。 */
public interface MarketingAdapter {

    /**
     * 活动单价（拼团价 / 秒杀价）。
     * 活动不存在、已停用、不在时间窗或不适用该 SKU 时返回 null（按原价计价）；
     * 活动存在但 rule_config 缺价时抛 BizException（配置错误不能静默按原价收）。
     */
    BigDecimal activityUnitPrice(Long activityId, Long skuId);

    /**
     * 活动单价（调用方已持有 SKU 所属 SPU 时用这个，省掉实现内部的一次 SKU 回查）。
     * 默认委托给两参版本，老调用方行为不变；实现方重写后即可跳过回查。
     * 混单下单时订单侧本来就把商品查出来了，没必要让营销侧再查一遍。 */
    default BigDecimal activityUnitPrice(Long activityId, Long skuId, Long spuId) {
        return activityUnitPrice(activityId, skuId);
    }

    /**
     * 优惠券抵扣额；不传券返回 0。
     * hasActivityPrice 为真时不得再用券（04 篇 §3.2.2 拼团价与券互斥，§4 步骤 2 券仅在无活动价时生效）。
     */
    BigDecimal couponDeduction(Long couponUserId, Long userId, BigDecimal itemAmount, boolean hasActivityPrice);

    /** 下单时锁定券（04 篇 §3.4：提交锁定、支付核销、取消/退款返还）。 */
    void lockCoupon(Long couponUserId, Long userId, Long orderId);

    /** 支付成功后核销。 */
    void useCoupon(Long orderId);

    /** 取消 / 退款时返还。 */
    void releaseCoupon(Long orderId);

    /**
     * 秒杀下单扣独立库存 + 每人限购校验（04 篇 §3.3）。
     * 非秒杀活动、活动不在进行中或未配独立库存时不动作；抢完/超限抛 BizException。
     */
    void reserveSeckillStock(Long activityId, Long userId, int qty);

    /** 秒杀订单取消 / 退款时归还独立库存。 */
    void releaseSeckillStock(Long activityId, int qty);
}
