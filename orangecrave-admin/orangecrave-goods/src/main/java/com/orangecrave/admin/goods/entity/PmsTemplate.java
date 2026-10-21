package com.orangecrave.admin.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_template")
public class PmsTemplate extends BaseEntity {

    private String templateName;
    private Long designerId;
    private Long categoryId;
    private String previewUrl;
    private String sourceUrl;
    private Integer status;
}
