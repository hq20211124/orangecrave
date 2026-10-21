package com.orangecrave.admin.marketing.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MktActivitySaveDTO {

    private Long id;
    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100, message = "活动名称过长")
    private String activityName;
    /** 1 拼团 / 2 秒杀 / 3 优惠券 / 4 邀请好友 */
    private Integer activityType;
    private String spuIds;
    private String skuIds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String ruleConfig;
    /** 0 停用 / 1 进行 / 2 结束 */
    private Integer status;
}
