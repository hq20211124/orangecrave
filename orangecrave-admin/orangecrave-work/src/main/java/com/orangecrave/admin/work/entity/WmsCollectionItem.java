package com.orangecrave.admin.work.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_collection_item")
public class WmsCollectionItem extends BaseEntity {

    private Long collectionId;
    private Long workId;
}
