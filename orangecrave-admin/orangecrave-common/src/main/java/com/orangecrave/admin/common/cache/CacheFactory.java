package com.orangecrave.admin.common.cache;

/**
 * 缓存工厂（见 00 篇支撑层）。Service 不再自己 new 缓存实现，而是注入本工厂按名字申请，
 * 于是「进程内」还是「跨实例共享」只由装配决定，业务代码一行不改。
 *
 * <p>name 是逻辑命名空间（如 {@code sys-config}、{@code pms-craft}）：Redis 实现把它拼进键前缀，
 * 让不同业务缓存互不覆盖、可整段失效。
 */
public interface CacheFactory {

    /** 按名字创建一个缓存实例。同一名字多次调用应指向同一命名空间。*/
    <K, V> Cache<K, V> create(String name);

    /**
     * 同上，但覆盖该缓存的条目存活时长（秒）。用于「结果查询」这类生命周期由业务约定的数据：
     * 秒杀下单结果只值得留几分钟，不该套用字典缓存那种长 TTL。
     */
    <K, V> Cache<K, V> create(String name, long ttlSeconds);
}
