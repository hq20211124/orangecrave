package com.orangecrave.admin.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款流水（见 01 篇 §9）。退款必须记独立流水，不直接冲减销售额。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fin_refund_ledger")
public class RefundLedger extends BaseEntity {

    private String refundNo;
    /** 1 订单退款 / 2 拼团失败退款 / 3 无料预付款退回 / 4 售后 */
    private Integer bizType;
    private Long bizId;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    /** 1 微信原路退回 */
    private Integer channel;
    /** 1 处理中 / 2 成功 / 3 失败 */
    private Integer status;
    /** 0 无需复核 / 1 待复核 / 2 复核通过 / 3 复核驳回（03 篇 §3.3 强制退款两段式） */
    private Integer reviewStatus;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private String reviewRemark;
    private String remark;
}
