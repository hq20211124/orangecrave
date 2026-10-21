package com.orangecrave.admin.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_coupon")
public class MktCoupon extends BaseEntity {

    private Long activityId;
    private String couponName;
    /** 1 满减 / 2 折扣 / 3 无门槛 */
    private Integer couponType;
    private BigDecimal threshold;
    private BigDecimal amount;
    private BigDecimal discount;
    private Integer validDays;
    private Integer totalQty;
    private Integer issuedQty;
}
