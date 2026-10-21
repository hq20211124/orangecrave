package com.orangecrave.admin.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 操作日志，含改前/改后快照（见 01 篇 §1）。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {

    private String operModule;
    private String operType;
    private String operDesc;
    private String method;
    private String requestUrl;
    private String requestParam;
    private String beforeData;
    private String afterData;
    private Long operUserId;
    private String operUserName;
    private String operIp;
    private LocalDateTime operTime;
    /** 0 失败 / 1 成功 */
    private Integer status;
    private String errorMsg;
}
