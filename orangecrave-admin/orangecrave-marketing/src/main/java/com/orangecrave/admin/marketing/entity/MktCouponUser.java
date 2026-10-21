package com.orangecrave.admin.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_coupon_user")
public class MktCouponUser extends BaseEntity {

    private Long couponId;
    private Long userId;
    private String code;
    /** 1 未使用 / 2 已使用 / 3 已过期 */
    private Integer status;
    private LocalDateTime useTime;
    private Long orderId;
    private LocalDateTime expireTime;
    /** 1 人工发放 / 2 自动发放 / 3 领券中心 */
    private Integer source;
}
