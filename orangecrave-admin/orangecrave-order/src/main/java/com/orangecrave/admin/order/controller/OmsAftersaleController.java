package com.orangecrave.admin.order.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.order.entity.OmsAftersale;
import com.orangecrave.admin.order.service.AftersaleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/admin-api/oms/aftersale")
@RequiredArgsConstructor
public class OmsAftersaleController {

    private final AftersaleService aftersaleService;

    @SaCheckPermission("oms:aftersale:list")
    @GetMapping("/page")
    public Result<PageResult<OmsAftersale>> page(PageQuery query) {
        return Result.ok(aftersaleService.page(query));
    }

    @GetMapping("/{id}")
    public Result<OmsAftersale> get(@PathVariable Long id) {
        return Result.ok(aftersaleService.get(id));
    }

    @SaCheckPermission("oms:aftersale:apply")
    @PostMapping("/apply")
    public Result<OmsAftersale> apply(@Valid @RequestBody ApplyDTO dto) {
        return Result.ok(aftersaleService.apply(dto.getOrderId(), dto.getOrderItemId(),
                dto.getType(), dto.getReason(), dto.getAmount()));
    }

    @SaCheckPermission("oms:aftersale:audit")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) AuditDTO dto) {
        aftersaleService.approve(id, StpUtil.getLoginIdAsLong(), dto == null ? null : dto.getRemark());
        return Result.ok();
    }

    @SaCheckPermission("oms:aftersale:audit")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody AuditDTO dto) {
        aftersaleService.reject(id, StpUtil.getLoginIdAsLong(), dto.getRemark());
        return Result.ok();
    }

    @Data
    public static class ApplyDTO {
        @NotNull(message = "订单号不能为空")
        private Long orderId;
        private Long orderItemId;
        @NotNull(message = "售后类型不能为空")
        @Min(value = 1, message = "售后类型不合法")
        @Max(value = 4, message = "售后类型不合法")
        private Integer type;
        @Size(max = 500, message = "原因过长")
        private String reason;
        @DecimalMin(value = "0.00", message = "退款金额不能为负")
        private BigDecimal amount;
    }

    @Data
    public static class AuditDTO {
        @Size(max = 500, message = "备注过长")
        private String remark;
    }
}
