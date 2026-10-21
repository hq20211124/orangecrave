package com.orangecrave.admin.audit.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("aud_image_audit")
public class AudImageAudit extends BaseEntity {

    /** 1 定制图 / 2 作品 / 3 头像 */
    private Integer bizType;
    private Long bizId;
    private String imageUrl;
    @TableField("pHash")
    private Long pHash;
    /** 自动。1 通过 / 2 疑似 / 3 违规 */
    private Integer autoResult;
    private String autoRemark;
    /** 人工。1 通过 / 2 驳回 */
    private Integer manualResult;
    private Long auditUserId;
    private LocalDateTime auditTime;
    private String auditRemark;
}
