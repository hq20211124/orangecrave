package com.orangecrave.admin.common.cache;

import java.util.function.Function;

/**
 * 缓存最小抽象（见 00 篇支撑层）。目标只有一个：让读多写少的字典数据
 * （{@code sys_config}、工艺字典）不必每次回查数据库。
 *
 * <p>失效由各 Service 的写路径显式触发，TTL 只是多实例部署下的兜底上限。
 * 第一批给进程内实现 {@link LocalCache}，第二批接入 Redis 后可换成跨实例共享的实现，
 * 调用方（Service）不需要改动。
 */
public interface Cache<K, V> {

    /**
     * 取缓存；未命中或已过期时用 loader 回源并写入。
     * loader 返回 null（例如配置项不存在）时不写缓存，避免把「查无」长期固化。
     */
    V get(K key, Function<K, V> loader);

    /** 使单个键失效（写路径调用）。*/
    void invalidate(K key);

    /** 清空全部（字典整体变更时调用）。*/
    void invalidateAll();

    /** 直接写入。用于状态会推进的场景（如异步任务结果 QUEUED→SUCCESS），不适合用 loader 表达。*/
    void put(K key, V value);
}
