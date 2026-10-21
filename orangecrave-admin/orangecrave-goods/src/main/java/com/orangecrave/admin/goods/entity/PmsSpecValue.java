package com.orangecrave.admin.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_spec_value")
public class PmsSpecValue extends BaseEntity {

    private Long groupId;
    private String valueName;
    private Integer sort;
}
