package com.orangecrave.admin.common.mq;

/**
 * 消息投递抽象（见 03 篇 §2.6 削峰）。业务侧只依赖这一层，因此「同步直投 / Redis Stream /
 * 将来换 RocketMQ、Kafka」是装配决定的事，改基础设施不动业务代码。
 *
 * <p>topic 是逻辑名（如 {@code order-submit}），实现负责拼上自己的前缀与键结构。
 */
public interface MessagePublisher {

    /** 投递一条消息。语义为「至少尝试投递一次」，实现各自的可靠性与时延特性见其类注释。 */
    void publish(String topic, String payload);
}
