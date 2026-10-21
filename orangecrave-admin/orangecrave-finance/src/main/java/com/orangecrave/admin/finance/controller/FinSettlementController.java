package com.orangecrave.admin.finance.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.finance.entity.FinDailySettlement;
import com.orangecrave.admin.finance.service.FinSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/fin/settlement")
@RequiredArgsConstructor
public class FinSettlementController {

    private final FinSettlementService settlementService;

    @SaCheckPermission("fin:settlement:list")
    @GetMapping("/page")
    public Result<PageResult<FinDailySettlement>> page(PageQuery query) {
        return Result.ok(settlementService.page(query));
    }

    @GetMapping("/reconcile")
    public Result<List<Map<String, Object>>> reconcile() {
        return Result.ok(settlementService.reconcile());
    }
}
