package com.orangecrave.admin.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order_item")
public class OmsOrderItem extends BaseEntity {

    private Long orderId;
    private String orderNo;
    private Long spuId;
    private Long skuId;
    private String goodsName;
    private String skuSpec;
    private String image;
    private Long workId;
    /** 本行活动 id（秒杀按行抢独立配额，取消 / 退款按行归还，见 04 篇 §3.3） */
    private Long activityId;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    /** 工艺与加价快照 JSON */
    private String craftSnapshot;
    /** 计价方式与参数快照 JSON */
    private String pricingSnapshot;
}
