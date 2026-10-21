package com.orangecrave.admin.common.mq;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;

/**
 * Stream 消费循环：单线程轮询消费者组，拉到的消息交给路由表分发。多实例部署时各实例
 * 同属一个组，同一条消息只会被其中一个实例处理——这正是削峰之后能水平扩容的原因。
 *
 * <p>失败不再重投：监听器（业务侧）有责任把失败结果写进结果存储，此处一律 ack，
 * 否则毒消息会把 pending 列表堆满。要做「失败重投」应引入 XAUTOCLAIM 重投环，
 * 属于下一阶段，不做半吊子实现。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "orangecrave.redis", name = "enabled", havingValue = "true")
public class RedisStreamConsumer {

    private final StringRedisTemplate redis;
    private final MessageProperties properties;
    private final MessageListenerRegistry registry;
    private final String consumerName;

    private volatile boolean running = true;
    private Thread worker;

    public RedisStreamConsumer(StringRedisTemplate redis, MessageProperties properties,
                               MessageListenerRegistry registry) {
        this.redis = redis;
        this.properties = properties;
        this.registry = registry;
        this.consumerName = resolveConsumerName(properties);
    }

    @PostConstruct
    public void start() {
        worker = new Thread(this::loop, "mq-stream-consumer");
        worker.setDaemon(true);
        worker.start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
    }

    private void loop() {
        // 首次拉取延后：让容器完成装配，避免在初始化期间提前解析监听器及其依赖
        sleep(properties.getInitialDelayMillis());
        while (running) {
            boolean idle = true;
            for (String topic : registry.topics()) {
                try {
                    idle &= poll(topic);
                } catch (Exception e) {
                    // 单 topic 拉取失败不影响其它 topic；Redis 恢复后下一轮自愈
                    log.warn("消息拉取失败 topic={}: {}", topic, e.getMessage());
                    idle = false;
                }
            }
            if (idle) {
                sleep(properties.getIdleSleepMillis());
            }
        }
    }

    /** @return 本次是否有消息被处理（false 表示有活干，立刻进入下一轮）。 */
    private boolean poll(String topic) {
        String key = properties.getStreamPrefix() + topic;
        if (!Boolean.TRUE.equals(redis.hasKey(key))) {
            // 该 topic 还没投过任何消息，Stream 尚不存在，此时建组 / 拉取都会报错
            return true;
        }
        ensureGroup(key);
        List<MapRecord<String, String, String>> records = stream().read(
                Consumer.from(properties.getGroup(), consumerName),
                StreamReadOptions.empty()
                        .count(properties.getBatchSize())
                        .block(Duration.ofMillis(properties.getBlockMillis())),
                StreamOffset.create(key, ReadOffset.lastConsumed()));
        if (records == null || records.isEmpty()) {
            return true;
        }
        for (MapRecord<String, String, String> record : records) {
            handle(topic, key, record);
        }
        // 消费完裁剪：Stream 只当通道用，历史消息不必长期留存
        stream().trim(key, properties.getMaxLength());
        return false;
    }

    private void handle(String topic, String key, MapRecord<String, String, String> record) {
        try {
            registry.dispatch(topic, record.getValue().get("payload"));
        } catch (Exception e) {
            log.error("消息处理失败 topic={} id={}: {}", topic, record.getId(), e.getMessage());
        } finally {
            stream().acknowledge(key, properties.getGroup(), record.getId());
        }
    }

    private void ensureGroup(String key) {
        try {
            stream().createGroup(key, ReadOffset.from("0"), properties.getGroup());
        } catch (Exception ignored) {
            // BUSYGROUP：组已存在（或另一实例刚建好）。拉取前已确认 Stream 存在，此处无需重试。
        }
    }

    private StreamOperations<String, String, String> stream() {
        return redis.opsForStream();
    }

    /** 未显式配置消费者名时按「主机名-进程号」生成，多实例部署无需为每台机器手工起名。 */
    private String resolveConsumerName(MessageProperties properties) {
        if (properties.getConsumerName() != null && !properties.getConsumerName().isBlank()) {
            return properties.getConsumerName();
        }
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "unknown";
        }
        return host + "-" + ProcessHandle.current().pid();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
