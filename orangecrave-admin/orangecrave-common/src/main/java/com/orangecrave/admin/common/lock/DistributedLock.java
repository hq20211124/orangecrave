package com.orangecrave.admin.common.lock;

import java.time.Duration;

/**
 * 分布式锁最小抽象（见 00 篇支撑层）。用于让 {@code @Scheduled} 兜底任务在多实例下
 * 只被一个节点执行。
 *
 * <p>第一批实现 {@link LocalDistributedLock}：单进程互斥，防同一 JVM 内定时任务重入重叠，
 * 多实例下不起作用。第二批接入 Redis 后由 SET NX PX + 持有者令牌的实现接管，
 * 调用方不带条件切换。
 */
public interface DistributedLock {

    /**
     * 尝试加锁。lease 为租约时长，持有者崩溃后锁自动过期，避免死锁；
     * 拿不到锁返回 false。
     */
    boolean tryLock(String key, Duration lease);

    /** 释放锁（只释放自己持有的）。*/
    void unlock(String key);
}
