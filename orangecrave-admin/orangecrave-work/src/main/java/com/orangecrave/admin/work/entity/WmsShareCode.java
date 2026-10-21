package com.orangecrave.admin.work.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分享码（见 01 篇 §4）。无料分享时分享者预付制品费，拆「已核销 / 未核销 / 已退」。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_share_code")
public class WmsShareCode extends BaseEntity {

    private String shareCode;
    private Long userId;
    private Long workId;
    private Long collectionId;
    /** 1 普通 / 2 无料 / 3 合集 */
    private Integer shareType;
    /** 1 领取者付商品费+邮费 / 2 分享者付商品费、领取者付邮费 */
    private Integer payerType;
    private Integer maxOrderQty;
    private Integer usedQty;
    private LocalDateTime expireTime;
    private Integer status;
    private String invalidReason;
    private BigDecimal prepayAmount;
    private BigDecimal prepayUsed;
    private BigDecimal prepayRefunded;
}
