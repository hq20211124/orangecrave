package com.orangecrave.admin.order.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.entity.OmsOrderItem;
import com.orangecrave.admin.order.entity.OmsOrderStatusLog;
import com.orangecrave.admin.order.model.OrderPriceDTO;
import com.orangecrave.admin.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/oms/order")
@RequiredArgsConstructor
public class OmsOrderController {

    private final OrderService orderService;

    @SaCheckPermission("oms:order:list")
    @GetMapping("/page")
    public Result<PageResult<OmsOrder>> page(PageQuery query, @RequestParam(required = false) Integer status) {
        return Result.ok(orderService.page(query, status));
    }

    @GetMapping("/{id}")
    public Result<OmsOrder> get(@PathVariable Long id) {
        return Result.ok(orderService.get(id));
    }

    @GetMapping("/{id}/items")
    public Result<List<OmsOrderItem>> items(@PathVariable Long id) {
        return Result.ok(orderService.items(id));
    }

    /** 状态流转时间线（客服答疑用）。**/
    @GetMapping("/{id}/timeline")
    public Result<List<OmsOrderStatusLog>> timeline(@PathVariable Long id) {
        return Result.ok(orderService.timeline(id));
    }

    /** 支付（演示：走支付桩）。 */
    @SaCheckPermission("oms:order:pay")
    @PutMapping("/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.ok();
    }

    @SaCheckPermission("order:status:update")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        orderService.manualUpdateStatus(id, dto.getStatus(), StpUtil.getLoginIdAsLong(), dto.getRemark());
        return Result.ok();
    }

    /** 改价（见 02 篇 §2.6，✱ 敏感，记快照）。 */
    @SaCheckPermission("order:price:update")
    @PutMapping("/{id}/price")
    public Result<Void> updatePrice(@PathVariable Long id, @Valid @RequestBody OrderPriceDTO dto) {
        orderService.updatePrice(id, dto, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    /** 备注（见 02 篇 §2.6）。 */
    @SaCheckPermission("oms:order:remark")
    @PutMapping("/{id}/remark")
    public Result<Void> updateRemark(@PathVariable Long id, @Valid @RequestBody RemarkDTO dto) {
        orderService.updateRemark(id, dto.getRemark());
        return Result.ok();
    }

    /** 批量导出（发货用，见 02 篇 §2.6）。 */
    @SaCheckPermission("oms:order:export")
    @PostMapping("/batch-export")
    public Result<List<Map<String, Object>>> batchExport(@Valid @RequestBody IdsDTO dto) {
        return Result.ok(orderService.exportForDelivery(dto.getIds()));
    }

    /**
     * 强制退款申请（见 03 篇 §3.3，✱ 敏感）。第一段：记快照 + 落一条待复核退款流水，
     * 钱不动；返回 fin_refund_ledger 主键，财务用它在 {@code /force-refund/{refundId}/review} 复核。
     */
    @SaCheckPermission("order:refund:force")
    @PostMapping("/{id}/force-refund")
    public Result<Long> forceRefund(@PathVariable Long id, @RequestBody(required = false) ForceRefundDTO dto) {
        BigDecimal amount = dto == null ? null : dto.getAmount();
        String reason = dto == null ? null : dto.getReason();
        return Result.ok(orderService.applyForceRefund(id, amount, reason, StpUtil.getLoginIdAsLong()));
    }

    /** 强制退款的第二段：财务复核（03 篇 §3.3 两段式）。驳回不改订单状态。 */
    @SaCheckPermission("order:refund:review")
    @PostMapping("/force-refund/{refundId}/review")
    public Result<String> reviewForceRefund(@PathVariable Long refundId, @Valid @RequestBody ReviewDTO dto) {
        return Result.ok(orderService.reviewForceRefund(refundId, Boolean.TRUE.equals(dto.getApproved()),
                dto.getRemark(), StpUtil.getLoginIdAsLong()));
    }

    @Data
    public static class ReviewDTO {
        @NotNull(message = "复核结论不能为空")
        private Boolean approved;
        @Size(max = 255, message = "复核意见过长")
        private String remark;
    }

    @Data
    public static class StatusDTO {
        @NotNull(message = "目标状态不能为空")
        private Integer status;
        @Size(max = 500, message = "备注过长")
        private String remark;
    }

    @Data
    public static class RemarkDTO {
        @Size(max = 500, message = "备注过长")
        private String remark;
    }

    @Data
    public static class IdsDTO {
        @NotEmpty(message = "请至少选择一个订单")
        private List<Long> ids;
    }

    @Data
    public static class ForceRefundDTO {
        @DecimalMin(value = "0.01", message = "退款金额必须大于 0")
        private BigDecimal amount;
        @Size(max = 500, message = "原因过长")
        private String reason;
    }
}
