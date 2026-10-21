package com.orangecrave.admin.common.limit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 进程内限流器工厂（未开启 Redis 时的默认实现）。单实例下行为正确；
 * 多实例部署时每个实例各有一份配额，实际放行速率约为「单机速率 × 实例数」。
 */
@Component
@ConditionalOnProperty(prefix = "orangecrave.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalRateLimiterFactory implements RateLimiterFactory {

    @Override
    public RateLimiter create(int permitsPerSecond, int burst) {
        return new LocalRateLimiter(permitsPerSecond, burst);
    }
}
