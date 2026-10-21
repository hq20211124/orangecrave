package com.orangecrave.admin.goods.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmsSkuSaveDTO {

    private Long id;
    @NotNull(message = "所属商品不能为空")
    private Long spuId;
    @NotBlank(message = "SKU 编码不能为空")
    @Size(max = 64, message = "SKU 编码过长")
    private String skuCode;
    /** 规格值组合 JSON 字符串 */
    private String specValueIds;
    @DecimalMin(value = "0.00", message = "价格不能为负")
    private BigDecimal price;
    /** 现货用；定制品不占库存 */
    private Integer stock;
    @DecimalMin(value = "0.00", message = "重量不能为负")
    private BigDecimal weight;
    @Size(max = 255, message = "图片地址过长")
    private String image;
    /** 物理尺寸，面积计价用（mm） */
    private Integer lengthMm;
    private Integer widthMm;
    private Integer status;
}
