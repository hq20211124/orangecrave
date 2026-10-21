package com.orangecrave.admin.goods.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PmsSpuSaveDTO {

    private Long id;
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称过长")
    private String spuName;
    @Size(max = 255, message = "副标题过长")
    private String subTitle;
    @Size(max = 255, message = "封面地址过长")
    private String cover;
    /** 图片 JSON 数组字符串 */
    private String images;
    private Long seriesId;
    private Long categoryId;
    /** 1 定制 / 2 现货 */
    private Integer goodsType;
    /** 1 按张 / 2 按面积 / 3 数量阶梯（见 04 篇 §2.1） */
    private Integer pricingType;
    @DecimalMin(value = "0.00", message = "基础价不能为负")
    private BigDecimal basePrice;
    /** 每平米单价（按面积时用） */
    @DecimalMin(value = "0.00", message = "面积单价不能为负")
    private BigDecimal areaPrice;
    /** 数量阶梯配置 JSON（pricing_type=3 时用） */
    private String pricingConfig;
    private Long designerId;
    private Integer minOrderQty;
    private Integer incrementQty;
    private Long freightTemplateId;
    /** 是否需打样才能分享 */
    private Integer needProof;
    /** 0 下架 / 1 上架 */
    private Integer status;
}
