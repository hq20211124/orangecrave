package com.orangecrave.admin.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("aud_sensitive_word")
public class AudSensitiveWord extends BaseEntity {

    private String word;
    private String category;
    /** 替换 / 拒绝 */
    private String action;
    private Integer status;
}
