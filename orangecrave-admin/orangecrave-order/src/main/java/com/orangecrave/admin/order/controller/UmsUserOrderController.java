package com.orangecrave.admin.order.controller;

import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户的订单（见 02 篇 §2.3 ums/user/{id}/orders）。
 * 置于 order 模块：user 模块看不到订单表，交叉查询在此承载。 */
@RestController
@RequestMapping("/admin-api/ums/user")
@RequiredArgsConstructor
public class UmsUserOrderController {

    private final OrderService orderService;

    @GetMapping("/{id}/orders")
    public Result<PageResult<OmsOrder>> orders(@PathVariable Long id, PageQuery query) {
        return Result.ok(orderService.mine(id, query));
    }
}
