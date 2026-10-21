package com.orangecrave.admin.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_aftersale")
public class OmsAftersale extends BaseEntity {

    private Long orderId;
    /** 支持按明细退 */
    private Long orderItemId;
    private String aftersaleNo;
    /** 1 退款 / 2 退货退款 / 3 色差 / 4 其他 */
    private Integer type;
    private String reason;
    private String description;
    private String images;
    private BigDecimal amount;
    /** 1 待审 / 2 通过 / 3 驳回 / 4 完成 */
    private Integer status;
    private Long auditUserId;
    private LocalDateTime auditTime;
    private String auditRemark;
    private String refundNo;
    private LocalDateTime refundTime;
}
