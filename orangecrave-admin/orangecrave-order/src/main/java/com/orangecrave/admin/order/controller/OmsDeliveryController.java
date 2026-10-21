package com.orangecrave.admin.order.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.order.entity.OmsDelivery;
import com.orangecrave.admin.order.service.DeliveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/oms/delivery")
@RequiredArgsConstructor
public class OmsDeliveryController {

    private final DeliveryService deliveryService;

    @SaCheckPermission("oms:delivery:ship")
    @PostMapping
    public Result<OmsDelivery> ship(@Valid @RequestBody ShipDTO dto) {
        return Result.ok(deliveryService.ship(dto.getOrderId(), dto.getCompany(), dto.getDeliveryNo(),
                StpUtil.getLoginIdAsLong()));
    }

    /** 批量发货（Excel 导入，见 02 篇 §2.6）。 */
    @SaCheckPermission("oms:delivery:batch")
    @PostMapping("/batch")
    public Result<List<OmsDelivery>> batchShip(@Valid @RequestBody List<DeliveryService.ShipItem> items) {
        return Result.ok(deliveryService.batchShip(items, StpUtil.getLoginIdAsLong()));
    }

    /** 电子面单打印数据（见 02 篇 §2.6）。 */
    @SaCheckPermission("oms:delivery:list")
    @GetMapping("/print")
    public Result<List<Map<String, Object>>> print(@RequestParam List<Long> orderIds) {
        return Result.ok(deliveryService.printWaybills(orderIds));
    }

    /** 物流轨迹（见 02 篇 §2.6）。 */
    @SaCheckPermission("oms:delivery:list")
    @GetMapping("/track")
    public Result<List<Map<String, Object>>> track(@RequestParam String no) {
        return Result.ok(deliveryService.track(no));
    }

    @SaCheckPermission("oms:delivery:list")
    @GetMapping("/{orderId}")
    public Result<OmsDelivery> byOrder(@PathVariable Long orderId) {
        return Result.ok(deliveryService.byOrder(orderId));
    }

    @Data
    public static class ShipDTO {
        @NotNull(message = "订单号不能为空")
        private Long orderId;
        @NotBlank(message = "快递公司不能为空")
        @Size(max = 50, message = "快递公司名过长")
        private String company;
        @NotBlank(message = "运单号不能为空")
        @Size(max = 64, message = "运单号过长")
        private String deliveryNo;
    }
}
