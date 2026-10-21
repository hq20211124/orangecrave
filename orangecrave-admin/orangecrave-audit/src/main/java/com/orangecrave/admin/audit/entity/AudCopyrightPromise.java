package com.orangecrave.admin.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 版权承诺（见 01 篇 §6 / 05 篇 §2.4）。每次上传都记一条。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("aud_copyright_promise")
public class AudCopyrightPromise extends BaseEntity {

    private Long userId;
    private Integer bizType;
    private Long bizId;
    private String promiseVersion;
    private String ip;
}
