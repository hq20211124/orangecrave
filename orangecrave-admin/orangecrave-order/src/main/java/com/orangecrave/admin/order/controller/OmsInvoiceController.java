package com.orangecrave.admin.order.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.order.entity.OmsInvoice;
import com.orangecrave.admin.order.model.OmsInvoiceSaveDTO;
import com.orangecrave.admin.order.service.InvoiceService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/oms/invoice")
@RequiredArgsConstructor
public class OmsInvoiceController {

    private final InvoiceService invoiceService;

    @SaCheckPermission("oms:invoice:list")
    @GetMapping("/page")
    public Result<PageResult<OmsInvoice>> page(PageQuery query) {
        return Result.ok(invoiceService.page(query));
    }

    @GetMapping("/{id}")
    public Result<OmsInvoice> get(@PathVariable Long id) {
        return Result.ok(invoiceService.get(id));
    }

    @SaCheckPermission("oms:invoice:apply")
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody OmsInvoiceSaveDTO dto) {
        invoiceService.apply(dto);
        return Result.ok();
    }

    @SaCheckPermission("oms:invoice:audit")
    @PostMapping("/{id}/issue")
    public Result<Void> issue(@PathVariable Long id) {
        invoiceService.issue(id);
        return Result.ok();
    }

    @SaCheckPermission("oms:invoice:audit")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        invoiceService.reject(id);
        return Result.ok();
    }

    @SaCheckPermission("oms:invoice:audit")
    @PostMapping("/{id}/upload")
    public Result<Void> upload(@PathVariable Long id, @RequestParam String fileUrl) {
        invoiceService.upload(id, fileUrl);
        return Result.ok();
    }
}
