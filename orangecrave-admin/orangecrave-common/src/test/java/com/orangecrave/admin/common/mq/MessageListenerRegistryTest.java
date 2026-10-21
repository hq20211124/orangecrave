package com.orangecrave.admin.common.mq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * 消息路由与同步直投测试（不依赖 Redis）：Redis 关闭时投递方就地调监听器，
 * 行为与「直接调用」一致，接口契约不变。
 */
class MessageListenerRegistryTest {

    /** 记录收到的 payload，便于断言投递确实抵达。 */
    private static final class CapturingListener implements MessageListener {
        private final String topic;
        private final List<String> received = new ArrayList<>();

        private CapturingListener(String topic) {
            this.topic = topic;
        }

        @Override
        public String topic() {
            return topic;
        }

        @Override
        public void onMessage(String payload) {
            received.add(payload);
        }
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<MessageListener> providerOf(MessageListener... listeners) {
        ObjectProvider<MessageListener> provider = mock(ObjectProvider.class);
        doAnswer(invocation -> {
            Consumer<MessageListener> consumer = invocation.getArgument(0);
            for (MessageListener listener : listeners) {
                consumer.accept(listener);
            }
            return null;
        }).when(provider).forEach(any());
        return provider;
    }

    @Test
    void inlinePublisherDeliversToListenerOfSameTopic() {
        CapturingListener listener = new CapturingListener("order-submit");
        MessageListenerRegistry registry = new MessageListenerRegistry(providerOf(listener));

        new InlineMessagePublisher(registry).publish("order-submit", "{\"requestId\":\"r1\"}");

        assertThat(listener.received).containsExactly("{\"requestId\":\"r1\"}");
        assertThat(registry.topics()).containsExactly("order-submit");
    }

    /** 没有监听器的 topic 只记日志、不抛异常：投递方不该因消费侧缺位而失败。 */
    @Test
    void unknownTopicIsDroppedSilently() {
        MessageListenerRegistry registry = new MessageListenerRegistry(providerOf());

        assertThatCode(() -> registry.dispatch("missing", "x")).doesNotThrowAnyException();
        assertThat(registry.topics()).isEmpty();
    }
}
