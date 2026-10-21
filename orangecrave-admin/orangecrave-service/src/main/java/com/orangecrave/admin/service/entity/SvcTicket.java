package com.orangecrave.admin.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("svc_ticket")
public class SvcTicket extends BaseEntity {

    private String ticketNo;
    private Long userId;
    /** 1 售后咨询 / 2 侵权投诉 / 3 色差投诉 / 4 其他 */
    private Integer type;
    /** 可空：咨询类常常还没下单（见 05 篇 §7.3） */
    private Long orderId;
    private String title;
    private String content;
    private String images;
    /** 1 待受理 / 2 处理中 / 3 已解决 / 4 已关闭**/
    private Integer status;
    private Long handlerId;
    /** 1 用户提交 / 2 客服代建 */
    private Integer source;
}
