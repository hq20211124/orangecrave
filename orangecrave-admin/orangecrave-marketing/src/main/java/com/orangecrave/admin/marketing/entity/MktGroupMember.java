package com.orangecrave.admin.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_group_member")
public class MktGroupMember extends BaseEntity {

    private Long groupId;
    private Long userId;
    private Long orderId;
    private Integer isLeader;
    /** 模拟成团时置 1 */
    private Integer isVirtual;
    private LocalDateTime joinTime;
    private Integer status;
}
