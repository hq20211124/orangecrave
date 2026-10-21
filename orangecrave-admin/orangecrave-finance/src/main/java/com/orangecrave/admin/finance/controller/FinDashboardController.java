package com.orangecrave.admin.finance.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.finance.service.FinDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/fin/dashboard")
@RequiredArgsConstructor
public class FinDashboardController {

    private final FinDashboardService dashboardService;

    @SaCheckPermission("fin:dashboard:view")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(dashboardService.overview());
    }

    @SaCheckPermission("fin:dashboard:todo")
    @GetMapping("/todo")
    public Result<Map<String, Object>> todo() {
        return Result.ok(dashboardService.todo());
    }

    @SaCheckPermission("fin:dashboard:view")
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend() {
        return Result.ok(dashboardService.trend());
    }
}
