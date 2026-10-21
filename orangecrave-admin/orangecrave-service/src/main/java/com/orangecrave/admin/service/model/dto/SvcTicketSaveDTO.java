package com.orangecrave.admin.service.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 客服代客建单入参（见 05 篇 §7）。工单号由服务端生成，状态与处理人只能走
 * /svc/ticket/{id}/status、/handler、/close 流转，不能在建单体里预设。 */
@Data
public class SvcTicketSaveDTO {

    private Long userId;
    /** 1 售后咨询 / 2 侵权投诉 / 3 色差投诉 / 4 其他 */
    private Integer type;
    /** 可空：咨询类常常还没下单 */
    private Long orderId;
    @Size(max = 200, message = "工单标题过长")
    private String title;
    @NotBlank(message = "工单内容不能为空")
    private String content;
    private String images;
    /** 1 用户提交 / 2 客服代建 */
    private Integer source;
}
