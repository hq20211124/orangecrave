package com.orangecrave.admin.task.handler;

import java.util.List;

/**
 * 延迟任务处理器（见 00 篇 §5.3）。业务模块实现它并注册为 Spring Bean。
 * 引擎负责计时与触发；处理逻辑必须自身幂等（配合状态守卫的条件 UPDATE）。 */
public interface DelayTaskHandler {

    /** 任务类型，如 ORDER_UNPAID_CANCEL */
    String taskType();

    /** 到点执行。必须幂等。 */
    void execute(Long bizId);

    /**
     * 兜底扫描的数据源：返回「已过期但可能未被触发」的业务主键。
     * 引擎每 5 分钟调用一次，堵住 DelayQueue 静默丢任务的缺口（00 篇 §5.3 第 ③ 层）。 */
    List<Long> scanOverdue();
}
