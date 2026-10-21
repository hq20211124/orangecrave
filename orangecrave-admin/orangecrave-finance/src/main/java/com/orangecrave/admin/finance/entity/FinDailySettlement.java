package com.orangecrave.admin.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 日结（见 01 篇 §9）。与微信支付 T+1 结算流水逐笔核对。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_daily_settlement")
public class FinDailySettlement extends BaseEntity {

    private LocalDate settleDate;
    /** 支付口径 */
    private BigDecimal orderAmount;
    /** 成团口径 */
    private BigDecimal grouponAmount;
    private BigDecimal refundAmount;
    private BigDecimal netAmount;
    private Integer orderCount;
}
