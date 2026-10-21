package com.orangecrave.admin.common.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * topic → 监听器 的路由表。监听器用 {@link ObjectProvider} 惰态收集：消费者线程在容器启动后
 * 才首次解析，避免在 Bean 初始化期间提前触发监听器（及其依赖的 Service）的创建。
 */
@Slf4j
@Component
public class MessageListenerRegistry {

    private final ObjectProvider<MessageListener> listenerProvider;
    private volatile Map<String, MessageListener> listeners;

    public MessageListenerRegistry(ObjectProvider<MessageListener> listenerProvider) {
        this.listenerProvider = listenerProvider;
    }

    /** 已注册监听的 topic 集合。 */
    public Set<String> topics() {
        return routeTable().keySet();
    }

    /** 按 topic 分发；没有监听器只记一条日志，不抛异常（投递方不应因消费侧缺位而失败）。 */
    public void dispatch(String topic, String payload) {
        MessageListener listener = routeTable().get(topic);
        if (listener == null) {
            log.warn("消息无监听器，已丢弃 topic={}", topic);
            return;
        }
        listener.onMessage(payload);
    }

    private Map<String, MessageListener> routeTable() {
        Map<String, MessageListener> local = listeners;
        if (local == null) {
            synchronized (this) {
                if (listeners == null) {
                    Map<String, MessageListener> map = new HashMap<>();
                    listenerProvider.forEach(l -> map.put(l.topic(), l));
                    listeners = map;
                }
                local = listeners;
            }
        }
        return local;
    }
}
