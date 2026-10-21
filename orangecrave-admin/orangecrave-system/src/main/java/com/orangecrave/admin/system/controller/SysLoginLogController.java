package com.orangecrave.admin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.system.entity.SysLoginLog;
import com.orangecrave.admin.system.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/system/login-log")
@RequiredArgsConstructor
public class SysLoginLogController {

    private final SysLogService logService;

    @SaCheckPermission("system:log:login")
    @GetMapping("/page")
    public Result<PageResult<SysLoginLog>> page(PageQuery query) {
        return Result.ok(logService.loginPage(query));
    }
}
