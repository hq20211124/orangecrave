package com.orangecrave.admin.service.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("svc_ticket_reply")
public class SvcTicketReply extends BaseEntity {

    private Long ticketId;
    /** 1 客服 / 2 用户 */
    private Integer replyType;
    private Long userId;
    private String content;
    private String images;
}
