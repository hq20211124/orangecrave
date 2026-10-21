package com.orangecrave.admin.order.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OrderSubmitDTO {

    private Long userId;
    @NotNull(message = "请选择商品规格")
    private Long skuId;
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer qty;
    private List<Long> craftIds;
    private Long channelId;
    /** 活动 id（拼团/秒杀），为空按原价计价 */
    private Long activityId;
    /** mkt_coupon_user 主键 */
    private Long couponUserId;
    /** 参团时所属的团（04 篇 §3.2） */
    private Long groupId;
    /** 无料领取时填写的分享码（03 篇 §7.3）：下单即核销套数与分享者预付的制品费 */
    @Size(max = 32, message = "分享码过长")
    private String shareCode;
    /** 收货地址快照 JSON */
    @Size(max = 2000, message = "地址快照过长")
    private String receiverSnapshot;
    /** 版权承诺版本（见 07 篇 §4.2） */
    @Size(max = 32, message = "承诺版本号过长")
    private String promiseVersion;
    /** 图槽位：一个明细下可挂多个「图-份数」对（单行简写用；混单时看 items） */
    @Valid
    @Size(max = 50, message = "图稿数量超出上限")
    private List<ImageSlot> images;
    /**
     * 混单行（03 篇 §1.3 一单多件）。传了就以它为准，上面的单行字段被忽略；
     * 不传则按单行字段组装一行，老调用方（小程序确认页）零改动。  */
    @Valid
    @Size(min = 1, max = 20, message = "订单行数须在 1-20 之间")
    private List<OrderItemInput> items;
}
