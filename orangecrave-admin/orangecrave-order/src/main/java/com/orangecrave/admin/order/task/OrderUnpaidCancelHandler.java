package com.orangecrave.admin.order.task;

import com.orangecrave.admin.order.constant.OrderStatus;
import com.orangecrave.admin.order.mapper.OmsOrderMapper;
import com.orangecrave.admin.order.service.OrderService;
import com.orangecrave.admin.task.handler.DelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 待付款超时取消（期限可配，见 00 篇 §5.3 / 03 篇 §2.5）。 */
@Component
@RequiredArgsConstructor
public class OrderUnpaidCancelHandler implements DelayTaskHandler {

    private final OmsOrderMapper orderMapper;
    private final OrderService orderService;

    @Override
    public String taskType() {
        return OrderService.TASK_UNPAID_CANCEL;
    }

    @Override
    public void execute(Long bizId) {
        orderService.cancel(bizId, "系统", null);
    }

    @Override
    public List<Long> scanOverdue() {
        return orderMapper.selectExpiredIds(OrderStatus.UNPAID);
    }
}
