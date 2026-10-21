package com.orangecrave.admin.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_craft")
public class PmsCraft extends BaseEntity {

    private String craftName;
    private String craftType;
    private BigDecimal addPrice;
    /** 1 固定 / 2 按面积**/
    private Integer addPriceType;
    private Integer status;
}
