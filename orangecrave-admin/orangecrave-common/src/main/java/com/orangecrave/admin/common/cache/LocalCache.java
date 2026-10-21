package com.orangecrave.admin.common.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 进程内缓存。字典表条目少、变更少，用最朴素的结构即可：每项带过期时间，
 * 读时惰性判过期；条目数超上限直接清空（字典表本就不该有多大，不做精细淘汰）。
 *
 * <p>多实例下失效只作用于本进程，所以必须配 TTL 兜底：一个节点改了字典，
 * 其他节点最迟在 TTL 内自愈。接入 Redis 后缓存跨实例共享，这个兜底就不再需要。
 */
public class LocalCache<K, V> implements Cache<K, V> {

    private record Entry<V>(V value, long expireAt) {
    }

    private final long ttlMillis;
    private final int maxSize;
    private final ConcurrentHashMap<K, Entry<V>> map = new ConcurrentHashMap<>();

    public LocalCache(long ttlMillis, int maxSize) {
        this.ttlMillis = ttlMillis;
        this.maxSize = maxSize;
    }

    @Override
    public V get(K key, Function<K, V> loader) {
        long now = System.currentTimeMillis();
        Entry<V> entry = map.get(key);
        if (entry != null && entry.expireAt() > now) {
            return entry.value();
        }
        V value = loader.apply(key);
        if (value == null) {
            return null;
        }
        if (map.size() >= maxSize) {
            map.clear();
        }
        map.put(key, new Entry<>(value, now + ttlMillis));
        return value;
    }

    @Override
    public void invalidate(K key) {
        map.remove(key);
    }

    @Override
    public void invalidateAll() {
        map.clear();
    }

    @Override
    public void put(K key, V value) {
        if (value == null) {
            map.remove(key);
            return;
        }
        if (map.size() >= maxSize) {
            map.clear();
        }
        map.put(key, new Entry<>(value, System.currentTimeMillis() + ttlMillis));
    }
}
