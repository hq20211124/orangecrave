package com.orangecrave.admin.common.mq;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 无 Redis 时的投递实现：就地同步调用监听器，不引入任何中间件与异步语义。
 *
 * <p>此时「削峰」能力不存在（请求线程直接干完活），但接口契约与功能完全一致——
 * 调用方拿到同一份返回值，只是结果在返回时已经就绪。测试与本地开发走这条路径。
 */
@Component
@ConditionalOnProperty(prefix = "orangecrave.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InlineMessagePublisher implements MessagePublisher {

    private final MessageListenerRegistry registry;

    public InlineMessagePublisher(MessageListenerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void publish(String topic, String payload) {
        registry.dispatch(topic, payload);
    }
}
