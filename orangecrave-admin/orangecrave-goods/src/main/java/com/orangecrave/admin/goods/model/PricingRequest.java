package com.orangecrave.admin.goods.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PricingRequest {

    private Long skuId;
    private Integer qty;
    /** 选中的工艺 id；为空表示不选任何工艺（不加价） */
    private List<Long> craftIds;
    /**
     * 活动单价（拼团价 / 秒杀价）。非空时覆盖 pricing_type 算出的单价，
     * 工艺加价照算（04 篇 §4 顺序 1「活动价覆盖原价」）。
     */
    private BigDecimal activityUnitPrice;
}
