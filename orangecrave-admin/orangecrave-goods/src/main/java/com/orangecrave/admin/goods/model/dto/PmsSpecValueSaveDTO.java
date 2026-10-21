package com.orangecrave.admin.goods.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PmsSpecValueSaveDTO {

    private Long id;
    @NotNull(message = "所属规格组不能为空")
    private Long groupId;
    @Size(max = 50, message = "规格值名称过长")
    private String valueName;
    private Integer sort;
}
