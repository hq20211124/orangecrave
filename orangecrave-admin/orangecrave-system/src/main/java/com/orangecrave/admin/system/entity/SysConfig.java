package com.orangecrave.admin.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统参数配置（见 01 篇 §1），key 对齐顾客端/selectParamsConfigByKey 机制。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    private String configKey;
    private String configValue;
    /** 0 内置 / 1 可改 */
    private Integer configType;
    private String remark;
}
