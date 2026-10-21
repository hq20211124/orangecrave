package com.orangecrave.admin.goods.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PmsSpecGroupSaveDTO {

    private Long id;
    @NotNull(message = "所属商品不能为空")
    private Long spuId;
    @Size(max = 50, message = "规格组名称过长")
    private String groupName;
    private Integer sort;
}
