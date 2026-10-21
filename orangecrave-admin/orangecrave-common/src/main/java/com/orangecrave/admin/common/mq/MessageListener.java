package com.orangecrave.admin.common.mq;

/**
 * 消息消费者。按 topic 与 {@link MessagePublisher} 配对：同一个 topic 只会有一个监听器
 * （多实例部署时它们抢同一个消费者组，同一条消息只会被其中一个实例处理）。
 *
 * <p>监听器内部应自行兜住异常并把结果落到结果存储里——抛出去意味着消息处理失败，
 * 消费侧会记日志，但不做无限重投（重投策略属于下一阶段）。
 */
public interface MessageListener {

    /** 该监听器订阅的 topic，需与投递方一致。 */
    String topic();

    /** 处理一条消息。payload 为投递时的字符串（本项目统一是 JSON）。 */
    void onMessage(String payload);
}
