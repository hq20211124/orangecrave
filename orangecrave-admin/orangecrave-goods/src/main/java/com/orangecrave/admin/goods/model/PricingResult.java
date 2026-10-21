package com.orangecrave.admin.goods.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 计价结果（见 04 篇 §2.2 完整计价流程）。  */
@Data
public class PricingResult {

    /** 面积（m²，4 位小数），按张计价时业 null */
    private BigDecimal area;
    /** 基础价：按 pricing_type 算出 */
    private BigDecimal basePrice;
    /** 工艺加价合计 */
    private BigDecimal craftPrice;
    /** 商品金额 = 基础价 + 工艺加价（券前，满额包邮按此判定，见 04 篇 §4） */
    private BigDecimal itemAmount;
    /** 优惠合计（目前只有优惠券抵扣；活动价已折进基础价） */
    private BigDecimal discountAmount;
    /** 运费 */
    private BigDecimal freight;
    /** 应付金额 = 商品金额 - 优惠 + 运费 */
    private BigDecimal payAmount;
}
