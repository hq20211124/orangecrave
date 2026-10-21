package com.orangecrave.admin.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 延迟任务日志（见 01 篇 §10）。唯一键 (task_type, biz_id) 用于幂等与去重。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tsk_delay_log")
public class TaskDelayLog extends BaseEntity {

    private String taskType;
    private Long bizId;
    private LocalDateTime fireTime;
    private LocalDateTime execTime;
    /** 1 待执行 / 2 成功 / 3 失败 */
    private Integer status;
    private Integer retryCount;
    private String errorMsg;
}
