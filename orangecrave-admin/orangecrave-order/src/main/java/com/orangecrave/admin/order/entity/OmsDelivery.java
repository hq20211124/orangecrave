package com.orangecrave.admin.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_delivery")
public class OmsDelivery extends BaseEntity {

    private Long orderId;
    private String deliveryCompany;
    private String deliveryNo;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    /** 1 已发货 / 2 已签收 / 3 退回 */
    private Integer status;
}
