package com.orangecrave.admin.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小程序用户（见 01 篇 §2）。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ums_user")
public class UmsUser extends BaseEntity {

    private String openid;
    private String unionid;
    private String nickname;
    private String avatar;
    private String phone;
    /** 0 普通 / 1 团长 / 2 摊主 */
    private Integer identityType;
    /** 0 未提交 / 1 待审 / 2 通过 / 3 驳回 */
    private Integer certStatus;
    private Long channelId;
    /** v1 不使用，仅留字段位**/
    private Integer vipLevel;
    /** v1 不使用，仅留字段位**/
    private Integer points;
}
