package com.orangecrave.admin.goods.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 批量改价入参：只允许改指定 sku 的价格，实体其余列不接受传入。 */
@Data
public class PmsSkuPriceDTO {

    @NotNull(message = "缺少规格 id")
    private Long id;
    @NotNull(message = "请填写价格")
    @DecimalMin(value = "0.00", message = "价格不能为负")
    private BigDecimal price;
}
