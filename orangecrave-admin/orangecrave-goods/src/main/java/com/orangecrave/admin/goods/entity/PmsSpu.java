package com.orangecrave.admin.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_spu")
public class PmsSpu extends BaseEntity {

    private String spuName;
    private String subTitle;
    private String cover;
    /** 图片 JSON 数组字符串 */
    private String images;
    private Long seriesId;
    private Long categoryId;
    /** 1 定制 / 2 现货 */
    private Integer goodsType;
    /** 1 按张 / 2 按面积 / 3 数量阶梯（见 04 篇 §2.1） */
    private Integer pricingType;
    private BigDecimal basePrice;
    /** 每平米单价（按面积时用） */
    private BigDecimal areaPrice;
    /** 数量阶梯配置 JSON（pricing_type=3 时用）：[{"minQty":1,"price":..}] */
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
