package com.orangecrave.admin.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_category")
public class PmsCategory extends BaseEntity {

    private String categoryName;
    private Long parentId;
    private Integer sort;
    private Integer status;
}
