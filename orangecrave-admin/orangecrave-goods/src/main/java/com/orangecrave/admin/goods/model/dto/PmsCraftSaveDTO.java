package com.orangecrave.admin.goods.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmsCraftSaveDTO {

    private Long id;
    @NotBlank(message = "工艺名称不能为空")
    @Size(max = 50, message = "工艺名称过长")
    private String craftName;
    @Size(max = 50, message = "工艺类型过长")
    private String craftType;
    @DecimalMin(value = "0.00", message = "加价金额不能为负")
    private BigDecimal addPrice;
    /** 1 固定 / 2 按面积 */
    private Integer addPriceType;
    private Integer status;
}
