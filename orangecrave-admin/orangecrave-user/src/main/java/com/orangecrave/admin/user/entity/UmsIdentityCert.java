package com.orangecrave.admin.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.crypto.EncryptedStringTypeHandler;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 身份认证记录（团长/摊主，见 01 篇 §2）。
 * autoResultMap 是让 id_card_no 的加解密 TypeHandler 在查询时生效。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ums_identity_cert", autoResultMap = true)
public class UmsIdentityCert extends BaseEntity {

    private Long userId;
    /** 1 团长 / 2 摊主 */
    private Integer identityType;
    private String realName;
    /** 加密存储，读写经 {@link EncryptedStringTypeHandler} 透明加解密 */
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String idCardNo;
    /** 证明材料图片 JSON */
    private String materialUrls;
    /** 1 待审 / 2 通过 / 3 驳回 */
    private Integer status;
    private Long auditUserId;
    private LocalDateTime auditTime;
    private String auditRemark;
    /** 是否已发打样券 */
    private Integer proofCouponGranted;
}
