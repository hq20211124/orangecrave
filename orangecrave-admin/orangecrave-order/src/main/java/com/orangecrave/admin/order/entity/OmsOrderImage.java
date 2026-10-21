package com.orangecrave.admin.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_order_image")
public class OmsOrderImage extends BaseEntity {

    private Long orderItemId;
    private Integer slotIndex;
    private String imageUrl;
    private Integer copies;
    @TableField("pHash")
    private Long pHash;
}
