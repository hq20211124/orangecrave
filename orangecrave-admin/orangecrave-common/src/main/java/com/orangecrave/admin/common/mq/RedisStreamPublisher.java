package com.orangecrave.admin.common.mq;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis Stream 投递实现：XADD 落一条消息即返回，落单等重活交给消费者线程做。
 * 调用方因此能立刻拿到「已受理」的答复，请求线程不必陪着做完全部业务。
 *
 * <p>用 Stream 而不是 Pub/Sub：Pub/Sub 消息不落地，消费者掉线期间的消息就永久丢了；
 * Stream 带消费者组与 pending 列表，消息落地、可事后补处理，这也是将来换成
 * RocketMQ / Kafka 时语义最接近的一层。
 */
@Component
@ConditionalOnProperty(prefix = "orangecrave.redis", name = "enabled", havingValue = "true")
public class RedisStreamPublisher implements MessagePublisher {

    private final StringRedisTemplate redis;
    private final MessageProperties properties;

    public RedisStreamPublisher(StringRedisTemplate redis, MessageProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public void publish(String topic, String payload) {
        redis.<String, String>opsForStream()
                .add(properties.getStreamPrefix() + topic, Map.of("payload", payload));
    }
}
