package com.orangecrave.admin.user.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 渠道增改入参（见 04 篇 §3.6）。只开放业务列，公共列与主键由服务端维护。 */
@Data
public class SysChannelSaveDTO {

    private Long id;
    @NotBlank(message = "渠道名称不能为空")
    @Size(max = 100, message = "渠道名称过长")
    private String channelName;
    @NotBlank(message = "渠道编码不能为空")
    @Size(max = 50, message = "渠道编码过长")
    private String channelCode;
    private Integer status;
}
