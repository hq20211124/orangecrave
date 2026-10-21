package com.orangecrave.admin.order.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 打样单分页（见 02 篇 §2.5 wms/proof/page）。
 * 打样单复用 oms_order，通过 order_type = 2 标记（见 01 篇 §4 wms_proof_order）。
 * 置于 order 模块：作品模块看不到订单表，交叉查询在此承载。 */
@RestController
@RequestMapping("/admin-api/wms/proof")
@RequiredArgsConstructor
public class WmsProofController {

    /** 打样单类型（见 03 篇 §1.1）。 */
    private static final int ORDER_TYPE_PROOF = 2;

    private final OrderService orderService;

    @SaCheckPermission("wms:proof:list")
    @GetMapping("/page")
    public Result<PageResult<OmsOrder>> page(PageQuery query) {
        return Result.ok(orderService.pageByType(query, ORDER_TYPE_PROOF));
    }
}
