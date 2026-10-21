package com.orangecrave.admin.common.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 缓存工厂（开启 Redis 时装配）。命名空间统一前缀 {@code oracle:cache:}，
 * 与 Sa-Token 会话键、分布式锁键互不干扰。
 */
@Component
@ConditionalOnProperty(prefix = "orangecrave.redis", name = "enabled", havingValue = "true")
public class RedisCacheFactory implements CacheFactory {

    private static final String KEY_PREFIX = "orangecrave:cache:";

    private final RedisTemplate<String, Object> redis;
    private final CacheProperties properties;

    public RedisCacheFactory(@Qualifier("orangecraveRedisTemplate") RedisTemplate<String, Object> redis,
                             CacheProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public <K, V> Cache<K, V> create(String name) {
        return new RedisCache<>(redis, KEY_PREFIX + name + ":", properties.getTtlSeconds());
    }

    @Override
    public <K, V> Cache<K, V> create(String name, long ttlSeconds) {
        return new RedisCache<>(redis, KEY_PREFIX + name + ":", ttlSeconds);
    }
}
