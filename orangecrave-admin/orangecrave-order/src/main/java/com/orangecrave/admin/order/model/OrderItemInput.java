package com.orangecrave.admin.order.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 混单的一行（03 篇 §1.3 一单多件）：一个 SKU + 数量 + 工艺 + 图槽位。
 * activityId 随行走，秒杀/活动价按行生效；券、团、分享码仍是整单维度。  */
@Data
public class OrderItemInput {

    @NotNull(message = "请选择商品规格")
    private Long skuId;
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer qty;
    private List<Long> craftIds;
    /** 活动 id（拼团/秒杀），为空按原价计价 */
    private Long activityId;
    /** 图槽位：一个明细下可挂多个「图-份数」对 */
    @Valid
    @Size(max = 50, message = "图稿数量超出上限")
    private List<ImageSlot> images;
}
