package com.orangecrave.admin.finance.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.finance.entity.RefundLedger;
import com.orangecrave.admin.finance.service.FinSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/fin/refund")
@RequiredArgsConstructor
public class FinRefundController {

    private final FinSettlementService settlementService;

    @SaCheckPermission("fin:refund:list")
    @GetMapping("/page")
    public Result<PageResult<RefundLedger>> page(PageQuery query) {
        return Result.ok(settlementService.refundPage(query));
    }

    /** 退款流水导出（见 02 篇 §2.10）。 */
    @SaCheckPermission("fin:refund:export")
    @GetMapping("/export")
    public Result<List<RefundLedger>> export() {
        return Result.ok(settlementService.refundList());
    }
}
