package com.orangecrave.admin.order.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.cache.CacheProperties;
import com.orangecrave.admin.common.cache.LocalCacheFactory;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.mq.MessagePublisher;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.model.OrderSubmitDTO;
import com.orangecrave.admin.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 异步下单受理端与消费者测试（03 篇 §2.6）。不依赖 Redis：结果存储用进程内实现，
 * 投递方用 mock——本测试只关心「受理 → 落结果」这条状态链是否正确。
 */
class AsyncOrderFlowTest {

    private final ObjectMapper json = new ObjectMapper();

    private OrderService orderService;
    private MessagePublisher publisher;
    private AsyncOrderResultStore resultStore;
    private OrderSubmitListener listener;
    private AsyncOrderService asyncOrderService;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        publisher = mock(MessagePublisher.class);
        resultStore = new AsyncOrderResultStore(new LocalCacheFactory(new CacheProperties()));
        listener = new OrderSubmitListener(orderService, resultStore, json);
        asyncOrderService = new AsyncOrderService(publisher, resultStore, json);
    }

    private OrderSubmitDTO dto() {
        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setSkuId(1L);
        dto.setQty(2);
        dto.setActivityId(2L);
        return dto;
    }

    private String payload(String requestId) throws Exception {
        return json.writeValueAsString(new OrderSubmitMessage(requestId, dto()));
    }

    /** 受理即返回，且此刻状态是 QUEUED——客户端据此知道该继续轮询。 */
    @Test
    void submitReturnsRequestIdAndMarksQueued() {
        String requestId = asyncOrderService.submit(dto());

        assertThat(requestId).hasSize(32);
        assertThat(asyncOrderService.result(requestId).getStatus()).isEqualTo(AsyncOrderResult.QUEUED);
        verify(publisher).publish(eq(OrderSubmitListener.TOPIC), anyString());
    }

    /** 未知 requestId 返回 null，供上层提示「结果不存在或已过期」。 */
    @Test
    void unknownRequestIdHasNoResult() {
        assertThat(asyncOrderService.result("nope")).isNull();
        assertThat(asyncOrderService.result(null)).isNull();
    }

    /** 消费成功：状态推进到 SUCCESS 并带上订单号，客户端可跳订单详情。 */
    @Test
    void listenerStoresOrderIdOnSuccess() throws Exception {
        OmsOrder order = new OmsOrder();
        order.setId(99L);
        when(orderService.submit(any())).thenReturn(order);

        listener.onMessage(payload("r1"));

        AsyncOrderResult result = resultStore.get("r1");
        assertThat(result.getStatus()).isEqualTo(AsyncOrderResult.SUCCESS);
        assertThat(result.getOrderId()).isEqualTo(99L);
    }

    /** 业务拒绝（库存抢完 / 超出限购）如实回给客户端，这类原因是可以展示的。 */
    @Test
    void listenerStoresBizReasonOnFailure() throws Exception {
        when(orderService.submit(any())).thenThrow(new BizException("秒杀库存已抢完"));

        listener.onMessage(payload("r2"));

        AsyncOrderResult result = resultStore.get("r2");
        assertThat(result.getStatus()).isEqualTo(AsyncOrderResult.FAILED);
        assertThat(result.getMessage()).isEqualTo("秒杀库存已抢完");
    }

    /** 非业务异常只留日志，回给客户端的是通用提示——内部细节不外泄。 */
    @Test
    void listenerHidesInternalErrors() throws Exception {
        when(orderService.submit(any())).thenThrow(new IllegalStateException("db connection reset"));

        listener.onMessage(payload("r3"));

        AsyncOrderResult result = resultStore.get("r3");
        assertThat(result.getStatus()).isEqualTo(AsyncOrderResult.FAILED);
        assertThat(result.getMessage()).doesNotContain("db connection reset");
    }

    /** 脏消息（坏 JSON / 缺字段）直接丢弃且不抛异常，否则消费循环会被一条毒消息打断。 */
    @Test
    void malformedPayloadIsDropped() {
        assertThatCode(() -> listener.onMessage("not-json")).doesNotThrowAnyException();
        assertThatCode(() -> listener.onMessage("{}")).doesNotThrowAnyException();
        assertThatCode(() -> listener.onMessage(null)).doesNotThrowAnyException();
    }
}
