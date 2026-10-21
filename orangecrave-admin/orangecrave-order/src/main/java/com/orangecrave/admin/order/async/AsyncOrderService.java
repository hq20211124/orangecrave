package com.orangecrave.admin.order.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.mq.MessagePublisher;
import com.orangecrave.admin.order.model.OrderSubmitDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 秒杀下单的受理端（见 03 篇 §2.6）。它只做三件事：发号（requestId）、标记受理中、把消息投出去，
 * 随后立刻返回——这正是削峰的意义：请求线程的耗时与下游是否拥堵无关。
 *
 * <p>真正落单见 {@link OrderSubmitListener}。普通下单不走这里，仍是同步的
 * {@code OrderService.submit}：没有峰值压力的入口引入异步只会徒增复杂度。
 */
@Service
@RequiredArgsConstructor
public class AsyncOrderService {

    private final MessagePublisher publisher;
    private final AsyncOrderResultStore resultStore;
    private final ObjectMapper objectMapper;

    /** 受理一笔秒杀下单，返回客户端用于轮询结果的 requestId。 */
    public String submit(OrderSubmitDTO dto) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        // 先落「排队中」再投递：即使投递后立刻被消费，也不会出现「结果先到、状态后到」
        resultStore.markQueued(requestId);
        publisher.publish(OrderSubmitListener.TOPIC, serialize(requestId, dto));
        return requestId;
    }

    /** 查询受理结果；未知或已过期的 requestId 返回 null。 */
    public AsyncOrderResult result(String requestId) {
        return requestId == null ? null : resultStore.get(requestId);
    }

    private String serialize(String requestId, OrderSubmitDTO dto) {
        try {
            return objectMapper.writeValueAsString(new OrderSubmitMessage(requestId, dto));
        } catch (JsonProcessingException e) {
            // 序列化失败属于编程错误而非业务错误，此处抛出让调用方立刻暴露问题
            throw new BizException("下单请求序列化失败: " + e.getMessage());
        }
    }
}
