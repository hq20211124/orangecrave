package com.orangecrave.admin.system.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysConfigDTO {

    private Long id;
    @NotBlank(message = "配置键不能为空")
    @Size(max = 100, message = "配置键过长")
    private String configKey;
    @NotBlank(message = "配置值不能为空")
    @Size(max = 500, message = "配置值过长")
    private String configValue;
    private Integer configType;
    @Size(max = 500, message = "备注过长")
    private String remark;
}
