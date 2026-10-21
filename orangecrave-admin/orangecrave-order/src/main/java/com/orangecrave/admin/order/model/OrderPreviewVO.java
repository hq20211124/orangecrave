package com.orangecrave.admin.order.model;

import com.orangecrave.admin.goods.model.PricingResult;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 计价预览响应（03 篇 §1.3 混单）：整单聚合字段继承自 PricingResult（老调用方读法不变），
 * lines 为逐行明细，供确认页按行展示小计。 */
@Data
public class OrderPreviewVO extends PricingResult {

    private List<Line> lines;

    /** 一行的商品快照与小计。 */
    @Data
    public static class Line {
        private Long skuId;
        private Long spuId;
        private String goodsName;
        private String image;
        private Integer qty;
        private BigDecimal area;
        private BigDecimal basePrice;
        private BigDecimal craftPrice;
        private BigDecimal itemAmount;
    }
}
