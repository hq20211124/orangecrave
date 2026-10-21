package com.orangecrave.admin.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 邀请好友记录（见 04 篇 §3.5）。
 * 01 篇未定义该表，此处按设计「被邀请人首次下单时记录邀请关系」补齐：
 * 拉新归因 + 双方奖励发放标记（奖励券走 mkt_coupon 发放）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_invite")
public class MktInvite extends BaseEntity {

    /** 关联邀请活动（activity_type = 4） */
    private Long activityId;
    /** 邀请人 */
    private Long inviterUserId;
    /** 被邀请人 */
    private Long inviteeUserId;
    /** 被邀请人首次下单的订单，可空（未生效时） */
    private Long orderId;
    /** 1 已记录 / 2 已生效（首单）/ 3 已失效 */
    private Integer status;
    /** 邀请人奖励是否已发 */
    private Integer inviterRewardGranted;
    /** 被邀请人奖励是否已发 */
    private Integer inviteeRewardGranted;
}
