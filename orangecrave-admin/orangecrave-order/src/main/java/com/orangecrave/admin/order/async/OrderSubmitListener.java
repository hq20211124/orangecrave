package com.orangecrave.admin.order.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.mq.MessageListener;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 异步下单消费者（见 03 篇 §2.6）。削峰之后真正干活的一侧：请求线程只负责受理，
 * 落单、扣库存、写明细、发延迟任务这一整套同步流程都挪到这个线程里串行执行。
 *
 * <p>失败不往上抛：{@link BizException} 是「库存抢完 / 超出限购」这类可预期的业务拒绝，
 * 把原因原样写进结果让客户端看到；其它异常只留日志、结果给一句通用提示，不泄漏内部细节。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSubmitListener implements MessageListener {

    public static final String TOPIC = "order-submit";

    private final OrderService orderService;
    private final AsyncOrderResultStore resultStore;
    private final ObjectMapper objectMapper;

    @Override
    public String topic() {
        return TOPIC;
    }

    @Override
    public void onMessage(String payload) {
        // 消息来自队列，是外部输入：脏内容一律丢弃并留痕，绝不让消费循环因一条坏消息中断
        if (payload == null || payload.isBlank()) {
            log.warn("秒杀下单消息为空，已丢弃");
            return;
        }
        OrderSubmitMessage message;
        try {
            message = objectMapper.readValue(payload, OrderSubmitMessage.class);
        } catch (JsonProcessingException e) {
            log.warn("秒杀下单消息解析失败，已丢弃: {}", e.getMessage());
            return;
        }
        if (message.getRequestId() == null || message.getDto() == null) {
            log.warn("秒杀下单消息缺少必要字段，已丢弃: {}", payload);
            return;
        }
        try {
            OmsOrder order = orderService.submit(message.getDto());
            resultStore.complete(message.getRequestId(), order.getId());
        } catch (BizException e) {
            resultStore.fail(message.getRequestId(), Objects.requireNonNullElse(e.getMessage(), "下单失败"));
        } catch (Exception e) {
            log.error("秒杀下单失败 requestId={}: {}", message.getRequestId(), e.getMessage());
            resultStore.fail(message.getRequestId(), "下单失败，请稍后重试");
        }
    }
}
