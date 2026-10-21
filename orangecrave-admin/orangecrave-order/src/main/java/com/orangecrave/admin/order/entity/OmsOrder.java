package com.orangecrave.admin.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order")
public class OmsOrder extends BaseEntity {

    private String orderNo;
    private Long userId;
    /** 1 普通 / 2 打样 / 3 无料领取 / 4 现货 */
    private Integer orderType;
    private Integer status;
    /** 实际付款人（无料时 ≠ userId） */
    private Long payerUserId;
    private BigDecimal totalAmount;
    private BigDecimal freightAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private BigDecimal refundAmount;
    /** 用掉的券，存 mkt_coupon_user 主键（用户持有的那张，不是券模板） */
    private Long couponId;
    private Long groupId;
    private Long shareCodeId;
    private Long activityId;
    private Long channelId;
    private LocalDateTime payTime;
    private Integer payType;
    private String transactionId;
    private LocalDateTime expireTime;
    /** 收货地址快照 JSON */
    private String receiverSnapshot;
    private String remark;
    /** MES 工单号（见 03 篇 §4.4；v1 由仓管手工录入） */
    private String workOrderNo;
    /** 预计交期（来自工厂日历，见 03 篇 §4.4） */
    private LocalDateTime expectedDelivery;
    /** MES 状态最近同步时间 */
    private LocalDateTime syncTime;
}
