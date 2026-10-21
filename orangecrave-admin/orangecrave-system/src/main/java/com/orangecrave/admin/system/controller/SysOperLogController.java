package com.orangecrave.admin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.system.entity.SysOperLog;
import com.orangecrave.admin.system.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/system/oper-log")
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysLogService logService;

    @SaCheckPermission("system:log:oper")
    @GetMapping("/page")
    public Result<PageResult<SysOperLog>> page(PageQuery query) {
        return Result.ok(logService.operPage(query));
    }

    /** 操作日志导出（见 02 篇 §2.2）。 */
    @SaCheckPermission("system:log:oper")
    @GetMapping("/export")
    public Result<List<SysOperLog>> export() {
        return Result.ok(logService.operList());
    }

    @SaCheckPermission("system:log:oper")
    @GetMapping("/{id}")
    public Result<SysOperLog> detail(@PathVariable Long id) {
        return Result.ok(logService.operDetail(id));
    }
}
