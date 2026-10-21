package com.orangecrave.admin.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_login_log")
public class SysLoginLog extends BaseEntity {

    private String userName;
    private String ip;
    private String browser;
    private String os;
    /** 0 失败 / 1 成功 */
    private Integer status;
    private String msg;
    private LocalDateTime loginTime;
}
