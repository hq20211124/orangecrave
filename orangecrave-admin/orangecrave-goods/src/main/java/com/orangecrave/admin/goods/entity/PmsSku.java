package com.orangecrave.admin.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_sku")
public class PmsSku extends BaseEntity {

    private Long spuId;
    private String skuCode;
    /** 规格值组合 JSON 字符串 */
    private String specValueIds;
    private BigDecimal price;
    /** 现货用；定制品不占库存**/
    private Integer stock;
    private BigDecimal weight;
    private String image;
    /** 物理尺寸，面积计价用（mm） */
    private Integer lengthMm;
    private Integer widthMm;
    private Integer status;
}
