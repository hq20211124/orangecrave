package com.orangecrave.admin.order.task;

import com.orangecrave.admin.order.constant.OrderStatus;
import com.orangecrave.admin.order.mapper.OmsOrderMapper;
import com.orangecrave.admin.order.service.OrderService;
import com.orangecrave.admin.task.handler.DelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待发货超时告警（00 篇 §5 / 03 篇 §2.5「仅告警，不改状态」）。
 * 告警渠道本系统只有后台待办：到期时打一条 warn 日志，超时单量由看板的 overdueDeliveryOrders 呈现；
 * 订单状态一律不动。 */
@Component
@RequiredArgsConstructor
public class PendingDeliveryAlertHandler implements DelayTaskHandler {

    public static final String TASK_DELIVERY_ALERT = OrderService.TASK_DELIVERY_ALERT;

    private final OmsOrderMapper orderMapper;
    private final OrderService orderService;

    @Override
    public String taskType() {
        return TASK_DELIVERY_ALERT;
    }

    @Override
    public void execute(Long bizId) {
        orderService.alertPendingDelivery(bizId);
    }

    @Override
    public List<Long> scanOverdue() {
        return orderMapper.selectPendingDeliveryBefore(OrderStatus.PENDING_DELIVERY,
                LocalDateTime.now().minusHours(orderService.pendingDeliveryAlertHours()));
    }
}
