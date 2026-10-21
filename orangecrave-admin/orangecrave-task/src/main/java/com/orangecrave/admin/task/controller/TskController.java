package com.orangecrave.admin.task.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.task.engine.DelayTaskEngine;
import com.orangecrave.admin.task.entity.TaskDelayLog;
import com.orangecrave.admin.task.mapper.TaskDelayLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 延迟任务（见 02 篇 §2.12）。  */
@RestController
@RequestMapping("/admin-api/tsk")
@RequiredArgsConstructor
public class TskController {

    private final TaskDelayLogMapper logMapper;
    private final DelayTaskEngine engine;

    @SaCheckPermission("system:log:task")
    @GetMapping("/delay-log/page")
    public Result<PageResult<TaskDelayLog>> page(PageQuery query) {
        Page<TaskDelayLog> page = logMapper.selectPage(query.toPage(), new LambdaQueryWrapper<TaskDelayLog>()
                .orderByDesc(TaskDelayLog::getId));
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @SaCheckPermission("system:log:task:retry")
    @PostMapping("/delay-log/{id}/retry")
    public Result<Void> retry(@PathVariable Long id) {
        TaskDelayLog record = logMapper.selectById(id);
        if (record != null) {
            engine.execute(record.getTaskType(), record.getBizId());
        }
        return Result.ok();
    }

    @SaCheckPermission("system:log:task:trigger")
    @PostMapping("/scan/trigger")
    public Result<Void> triggerScan() {
        engine.scan();
        return Result.ok();
    }
}
