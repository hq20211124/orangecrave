package com.orangecrave.admin.goods.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PmsCategorySaveDTO {

    private Long id;
    @NotBlank(message = "品类名称不能为空")
    @Size(max = 100, message = "品类名称过长")
    private String categoryName;
    private Long parentId;
    private Integer sort;
    private Integer status;
}
