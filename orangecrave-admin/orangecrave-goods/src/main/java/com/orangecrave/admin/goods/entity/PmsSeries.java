package com.orangecrave.admin.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_series")
public class PmsSeries extends BaseEntity {

    private String seriesName;
    private String cover;
    private String ipName;
    private Integer sort;
    private Integer status;
}
