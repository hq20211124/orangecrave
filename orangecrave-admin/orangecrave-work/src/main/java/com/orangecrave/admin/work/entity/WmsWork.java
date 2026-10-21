package com.orangecrave.admin.work.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 作品（见 01 篇 §4）。有独立状态机的一等实体。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_work")
public class WmsWork extends BaseEntity {

    private Long userId;
    private String workName;
    private String coverUrl;
    private Long spuId;
    private Long skuId;
    /** 角 WorkStatus */
    private Integer status;
    private String auditRemark;
    private LocalDateTime expireTime;
    /** 若来自他人分享 */
    private Long sourceShareId;
    /** 分享中不可删除 */
    private Integer deletedShared;
}
