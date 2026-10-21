package com.orangecrave.admin.goods.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PmsDesignerSaveDTO {

    private Long id;
    @NotBlank(message = "画师名称不能为空")
    @Size(max = 100, message = "画师名称过长")
    private String designerName;
    @Size(max = 255, message = "头像地址过长")
    private String avatar;
    @Size(max = 255, message = "简介过长")
    private String intro;
    private Integer status;
}
