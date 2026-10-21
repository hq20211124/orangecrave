package com.orangecrave.admin.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收货地址（见 01 篇 §2）。订单不引用此表，只存快照。* */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ums_address")
public class UmsAddress extends BaseEntity {

    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Integer isDefault;
}
