package com.orangecrave.admin.common.limit;

/**
 * 令牌桶限流（见支撑层）。用途是把入口流量压到后端能承受的量级：令牌没了就快速失败，
 * 让请求在触达业务与数据库之前被挡回去，而不是把连接池和线程全占满后一起慢。
 *
 * <p>实现按 Redis 开关切换：开启时桶在 Redis 里（多实例共享同一份配额，扩容不会线性放大总流量），
 * 关闭时桶在进程内（单实例有效，多实例下实际速率会被放大）。
 */
public interface RateLimiter {

    /** 申请一个令牌；拿不到返回 false（调用方据此快速失败）。 */
    boolean tryAcquire(String key);
}
