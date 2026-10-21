package com.orangecrave.admin.goods.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PmsTemplateSaveDTO {

    private Long id;
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称过长")
    private String templateName;
    private Long designerId;
    private Long categoryId;
    @Size(max = 255, message = "预览图地址过长")
    private String previewUrl;
    @Size(max = 255, message = "源文件地址过长")
    private String sourceUrl;
    private Integer status;
}
