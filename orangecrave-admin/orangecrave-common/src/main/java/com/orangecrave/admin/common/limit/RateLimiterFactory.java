package com.orangecrave.admin.common.limit;

/**
 * 限流器工厂。限流参数是「策略」不是「状态」：每个策略（一条路径前缀）建一个限流器实例，
 * 实例内部各自维护桶。因此调用方拿到限流器后只管问「这个身份还能不能过」。
 */
public interface RateLimiterFactory {

    /** 每秒补充 permitsPerSecond 个令牌，桶容量 burst（允许的瞬时突发量）。 */
    RateLimiter create(int permitsPerSecond, int burst);
}
