package com.orangecrave.admin.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 活动（见 01 篇 §7）。拼团/秒杀/优惠券/邀请好友共用一张表，靠 activity_type 区分。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_activity")
public class MktActivity extends BaseEntity {

    private String activityName;
    /** 1 拼团 / 2 秒杀 / 3 优惠券 / 4 邀请好友**/
    private Integer activityType;
    private String spuIds;
    private String skuIds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 各自类型的规则 JSON */
    private String ruleConfig;
    /** 0 停用 / 1 进行 / 2 结束 */
    private Integer status;
    /** 秒杀独立库存配额（04 篇 §3.3）：与 SKU 库存分离，null 表示未配额、不限量 */
    private Integer totalStock;
    /** 秒杀已抢数量，只由下单扣、取消还，客户端不可写 */
    private Integer soldStock;
}
