package com.orangecrave.admin.order.task;

import com.orangecrave.admin.order.constant.OrderStatus;
import com.orangecrave.admin.order.mapper.OmsOrderMapper;
import com.orangecrave.admin.order.service.OrderService;
import com.orangecrave.admin.system.entity.SysConfig;
import com.orangecrave.admin.system.service.SysConfigService;
import com.orangecrave.admin.task.handler.DelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动确认收货（默认 10 天，可配，见 00 篇 §5.3 / 03 篇 §2.5）。 */
@Component
@RequiredArgsConstructor
public class OrderAutoConfirmHandler implements DelayTaskHandler {

    public static final String TASK_AUTO_CONFIRM = "ORDER_AUTO_CONFIRM";

    private final OmsOrderMapper orderMapper;
    private final OrderService orderService;
    private final SysConfigService configService;

    @Override
    public String taskType() {
        return TASK_AUTO_CONFIRM;
    }

    @Override
    public void execute(Long bizId) {
        orderService.confirm(bizId, "系统", null);
    }

    @Override
    public List<Long> scanOverdue() {
        int days = 10;
        try {
            SysConfig config = configService.getByKey("autoConfirmDays");
            days = Integer.parseInt(config.getConfigValue().trim());
        } catch (Exception ignored) {
            // 用默认值
        }
        return orderMapper.selectDeliveredBefore(OrderStatus.DELIVERED, LocalDateTime.now().minusDays(days));
    }
}
