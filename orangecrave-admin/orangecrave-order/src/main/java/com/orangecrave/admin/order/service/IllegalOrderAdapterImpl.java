package com.orangecrave.admin.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orangecrave.admin.order.constant.OrderStatus;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.mapper.OmsOrderMapper;
import com.orangecrave.admin.user.adapter.IllegalOrderAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 团长/摊主认证被驳回 → 其未进入生产的订单按非法单收口（03 篇 §2.1 状态 13）。 */
@Service
@RequiredArgsConstructor
public class IllegalOrderAdapterImpl implements IllegalOrderAdapter {

    private final OmsOrderMapper orderMapper;
    private final OrderService orderService;

    @Override
    public int cancelIllegalOrders(Long userId, String remark) {
        if (userId == null) {
            return 0;
        }
        List<OmsOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getUserId, userId)
                .in(OmsOrder::getStatus, OrderStatus.UNPAID, OrderStatus.PENDING_GROUP, OrderStatus.PENDING_AUDIT));
        int cancelled = 0;
        for (OmsOrder order : orders) {
            if (orderService.rejectIllegal(order.getId(), remark)) {
                cancelled++;
            }
        }
        return cancelled;
    }
}
