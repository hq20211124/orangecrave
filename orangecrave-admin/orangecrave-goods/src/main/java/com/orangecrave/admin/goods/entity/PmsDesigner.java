package com.orangecrave.admin.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_designer")
public class PmsDesigner extends BaseEntity {

    private String designerName;
    private String avatar;
    private String intro;
    private Integer status;
}
