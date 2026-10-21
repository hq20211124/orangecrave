package com.orangecrave.admin.marketing.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀活动入参：不含 activity_type，该列由服务端固定为秒杀，
 * 否则持有 mkt:seckill:update 权限就能把别的活动类型改写到秒杀接口上。 */
@Data
public class MktSeckillSaveDTO {

    private Long id;
    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100, message = "活动名称过长")
    private String activityName;
    private String spuIds;
    private String skuIds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String ruleConfig;
    /** 0 停用 / 1 进行 / 2 结束 */
    private Integer status;
    /** 秒杀独立库存配额（04 篇 §3.3）；不传表示不限量。已售数量 sold_stock 只由下单扣减，客户端写不进来 */
    @Min(value = 0, message = "秒杀库存不能为负")
    private Integer totalStock;
}
