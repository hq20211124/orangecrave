package com.orangecrave.admin.common.limit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 限流器工厂（开启 Redis 时装配）：桶在 Redis，多实例共用一份配额。
 */
@Component
@ConditionalOnProperty(prefix = "orangecrave.redis", name = "enabled", havingValue = "true")
public class RedisRateLimiterFactory implements RateLimiterFactory {

    private final StringRedisTemplate redis;

    public RedisRateLimiterFactory(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public RateLimiter create(int permitsPerSecond, int burst) {
        return new RedisRateLimiter(redis, permitsPerSecond, burst);
    }
}
