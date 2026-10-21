package com.orangecrave.admin.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_freight_template")
public class PmsFreightTemplate extends BaseEntity {

    private String templateName;
    /** 1 按件 / 2 按重 / 3 按地区 */
    private Integer chargeType;
    private Integer firstUnit;
    private BigDecimal firstFee;
    private Integer extraUnit;
    private BigDecimal extraFee;
    /** 满额包邮阈值**/
    private BigDecimal freeThreshold;
    /** 偏远地区规则 JSON */
    private String regionRules;
}
