package com.orangecrave.admin.common.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 单机缓存工厂（未开启 Redis 时的默认实现）。TTL 在这里是「失效只达本进程」的兜底：
 * 一个节点改了字典，其他节点最迟在 TTL 内自愈。
 */
@Component
@ConditionalOnProperty(prefix = "orangecrave.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalCacheFactory implements CacheFactory {

    private final CacheProperties properties;

    public LocalCacheFactory(CacheProperties properties) {
        this.properties = properties;
    }

    @Override
    public <K, V> Cache<K, V> create(String name) {
        return new LocalCache<>(properties.ttlMillis(), properties.getMaxSize());
    }

    @Override
    public <K, V> Cache<K, V> create(String name, long ttlSeconds) {
        return new LocalCache<>(ttlSeconds * 1000L, properties.getMaxSize());
    }
}
