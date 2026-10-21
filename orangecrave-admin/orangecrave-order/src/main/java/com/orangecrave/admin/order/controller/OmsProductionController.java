package com.orangecrave.admin.order.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.order.constant.MesStatus;
import com.orangecrave.admin.order.constant.OrderStatus;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/oms/production")
@RequiredArgsConstructor
public class OmsProductionController {

    private final OrderService orderService;

    /** 生产工单分页（待生产/生产中/质检）。**/
    @SaCheckPermission("oms:production:list")
    @GetMapping("/page")
    public Result<PageResult<OmsOrder>> page(PageQuery query, @RequestParam Integer status) {
        return Result.ok(orderService.page(query, status));
    }

    /** 推进生产状态（仓管）。**/
    @SaCheckPermission("oms:production:advance")
    @PutMapping("/{id}/status")
    public Result<Void> advance(@PathVariable Long id) {
        orderService.advanceProduction(id, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    /** 审核放行：待审核 → 待生产。 */
    @SaCheckPermission("oms:production:audit")
    @PutMapping("/{id}/audit-pass")
    public Result<Void> auditPass(@PathVariable Long id) {
        orderService.auditPass(id, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    /** 录入 MES 工单号（见 02 篇 §2.6 / 03 篇 §4.4）。 */
    @SaCheckPermission("oms:production:work-order")
    @PostMapping("/{id}/work-order-no")
    public Result<Void> workOrderNo(@PathVariable Long id, @Valid @RequestBody WorkOrderNoDTO dto) {
        orderService.setWorkOrderNo(id, dto.getWorkOrderNo(), dto.getExpectedDelivery());
        return Result.ok();
    }

    /**
     * 从 MES 同步生产状态（v1 手工触发）。
     * <p>带 {@code mesStatus} 时按 03 篇 §4.3 的车间术语表推进订单，返回给用户看的进度文案。
     */
    @SaCheckPermission("oms:production:sync")
    @PostMapping("/sync")
    public Result<String> sync(@RequestParam Long orderId,
                               @RequestBody(required = false) MesSyncDTO dto) {
        return Result.ok(orderService.syncProduction(orderId, dto == null ? null : dto.getMesStatus()));
    }

    /** MES 车间术语 → 订单状态 + 用户文案（见 03 篇 §4.3），前端下拉用。 */
    @SaCheckPermission("oms:production:sync")
    @GetMapping("/mes-status")
    public Result<Map<String, Map<String, Object>>> mesStatus() {
        Map<String, Map<String, Object>> table = new LinkedHashMap<>();
        MesStatus.table().forEach((k, v) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderStatus", v.orderStatus());
            row.put("userText", v.userText());
            table.put(k, row);
        });
        return Result.ok(table);
    }

    @Data
    public static class WorkOrderNoDTO {
        @NotBlank(message = "MES 工单号不能为空")
        @Size(max = 64, message = "工单号过长")
        private String workOrderNo;
        private LocalDateTime expectedDelivery;
    }

    @Data
    public static class MesSyncDTO {
        @Size(max = 32, message = "MES 状态过长")
        private String mesStatus;
    }
}
