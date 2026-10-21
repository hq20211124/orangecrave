package com.orangecrave.admin.work.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 作品图槽位（见 01 篇 §4）。一个作品下挂多个「图 + 份数 + pHash」。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_work_image")
public class WmsWorkImage extends BaseEntity {

    private Long workId;
    private Integer slotIndex;
    private String imageUrl;
    private String thumbUrl;
    private Integer copies;
    @TableField("pHash")
    private Long pHash;
}
