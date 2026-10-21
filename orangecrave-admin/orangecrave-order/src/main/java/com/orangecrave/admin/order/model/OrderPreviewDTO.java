package com.orangecrave.admin.order.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OrderPreviewDTO {

    /** 由 Controller 从会话回填，不接受客户端传入（优惠券归属校验用） */
    private Long userId;
    @NotNull(message = "请选择商品规格")
    private Long skuId;
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer qty;
    private List<Long> craftIds;
    /** 活动 id（拼团/秒杀），为空按原价计价 */
    private Long activityId;
    /** mkt_coupon_user 主键，即用户持有的那张券 */
    private Long couponUserId;
    /**
     * 混单行（03 篇 §1.3）。传了就以它为准，上面的单行字段被忽略；
     * 不传则按单行字段组装一行，老调用方（小程序确认页）零改动。  */
    @Valid
    @Size(min = 1, max = 20, message = "订单行数须在 1-20 之间")
    private List<OrderItemInput> items;
}
