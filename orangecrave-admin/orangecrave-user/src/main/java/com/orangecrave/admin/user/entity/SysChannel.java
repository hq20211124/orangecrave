package com.orangecrave.admin.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 渠道（见 01 篇 §2）。小红书等流量与订单归因。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_channel")
public class SysChannel extends BaseEntity {

    private String channelName;
    private String channelCode;
    private Integer status;
}
