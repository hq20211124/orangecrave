package com.orangecrave.admin.goods.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmsFreightTemplateSaveDTO {

    private Long id;
    @NotBlank(message = "运费模板名称不能为空")
    @Size(max = 100, message = "运费模板名称过长")
    private String templateName;
    /** 1 按件 / 2 按重 / 3 按地区 */
    private Integer chargeType;
    private Integer firstUnit;
    @DecimalMin(value = "0.00", message = "首件运费不能为负")
    private BigDecimal firstFee;
    private Integer extraUnit;
    @DecimalMin(value = "0.00", message = "续件运费不能为负")
    private BigDecimal extraFee;
    /** 满额包邮阈值 */
    @DecimalMin(value = "0.00", message = "包邮阈值不能为负")
    private BigDecimal freeThreshold;
    /** 偏远地区规则 JSON */
    private String regionRules;
}
