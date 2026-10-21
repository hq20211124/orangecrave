package com.orangecrave.admin.work.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_collection")
public class WmsCollection extends BaseEntity {

    private Long userId;
    private String collectionName;
    private String coverUrl;
    private Integer status;
}
