package com.orangecrave.admin.audit.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 违规图库（见 01 篇 §6）。pHash 比对是廉价且真正有用的第一层拦截。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("aud_blacklist")
public class AudBlacklist extends BaseEntity {

    @TableField("pHash")
    private Long pHash;
    private String source;
    private String remark;
    private Integer status;
}
