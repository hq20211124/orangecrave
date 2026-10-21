package com.orangecrave.admin.common.cache;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Set;
import java.util.function.Function;

/**
 * 跨实例共享缓存。键 = 命名空间 + 业务键，值走 Jackson 序列化，因此任一台后台实例
 * 改了字典，其他实例下一次读即见新值，不再依赖 TTL 兜底。
 *
 * <p>{@link #invalidateAll()} 用 {@code KEYS} 扫命名空间：字典表条目极少且整体失效是低频动作，
 * 一次扫描的代价可以接受；换量级更大的缓存时应改成 SCAN 游标遍历。
 */
public class RedisCache<K, V> implements Cache<K, V> {

    private final RedisTemplate<String, Object> redis;
    private final String namespace;
    private final long ttlSeconds;

    public RedisCache(RedisTemplate<String, Object> redis, String namespace, long ttlSeconds) {
        this.redis = redis;
        this.namespace = namespace;
        this.ttlSeconds = ttlSeconds;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(K key, Function<K, V> loader) {
        String redisKey = redisKey(key);
        Object cached = redis.opsForValue().get(redisKey);
        if (cached != null) {
            return (V) cached;
        }
        V value = loader.apply(key);
        if (value != null) {
            redis.opsForValue().set(redisKey, value, Duration.ofSeconds(ttlSeconds));
        }
        return value;
    }

    @Override
    public void invalidate(K key) {
        redis.delete(redisKey(key));
    }

    @Override
    public void invalidateAll() {
        Set<String> keys = redis.keys(namespace + "*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @Override
    public void put(K key, V value) {
        if (value == null) {
            redis.delete(redisKey(key));
            return;
        }
        redis.opsForValue().set(redisKey(key), value, Duration.ofSeconds(ttlSeconds));
    }

    private String redisKey(K key) {
        return namespace + key;
    }
}
