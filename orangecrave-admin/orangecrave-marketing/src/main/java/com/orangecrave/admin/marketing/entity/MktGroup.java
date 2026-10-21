package com.orangecrave.admin.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_group")
public class MktGroup extends BaseEntity {

    private Long activityId;
    private Long leaderUserId;
    private Integer targetSize;
    private Integer currentSize;
    /** 1 拼团中 / 2 已成团 / 3 拼团失败 */
    private Integer status;
    private LocalDateTime expireTime;
    /** 是否模拟成团 */
    private Integer isSimulated;
}
