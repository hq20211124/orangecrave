package com.orangecrave.admin.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order_status_log")
public class OmsOrderStatusLog extends BaseEntity {

    private Long orderId;
    private Integer fromStatus;
    private Integer toStatus;
    /** 系统 / 管理员 / 用户 */
    private String operatorType;
    private Long operatorId;
    private String remark;
}
