package com.orangecrave.admin.common.limit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 令牌桶语义测试。用可控时钟推进时间，不依赖 Thread.sleep，因此结果确定、不挑机器。
 */
class LocalRateLimiterTest {

    private long nowNanos;

    private RateLimiter limiter(int permitsPerSecond, int burst) {
        return new LocalRateLimiter(permitsPerSecond, burst, () -> nowNanos);
    }

    private void advanceMillis(long millis) {
        nowNanos += millis * 1_000_000L;
    }

    /** 先耗尽突发额度，再按速率补充：这正是「瞬间放行一批、之后按速率放行」的令牌桶语义。 */
    @Test
    void burstThenRefillByRate() {
        RateLimiter limiter = limiter(2, 2);

        assertThat(limiter.tryAcquire("u1")).isTrue();
        assertThat(limiter.tryAcquire("u1")).isTrue();
        assertThat(limiter.tryAcquire("u1")).isFalse();

        advanceMillis(500);
        assertThat(limiter.tryAcquire("u1")).isTrue();
        assertThat(limiter.tryAcquire("u1")).isFalse();
    }

    /** 桶按身份分片：一个用户被限住不影响另一个用户。 */
    @Test
    void identitiesHaveIndependentBuckets() {
        RateLimiter limiter = limiter(1, 1);

        assertThat(limiter.tryAcquire("u1")).isTrue();
        assertThat(limiter.tryAcquire("u1")).isFalse();
        assertThat(limiter.tryAcquire("u2")).isTrue();
    }

    /** 长时间空闲也只补到桶容量，不会攒出无限额度（否则一次突发就能打穿后端）。 */
    @Test
    void refillIsCappedAtBurst() {
        RateLimiter limiter = limiter(10, 3);
        advanceMillis(60_000);

        int allowed = 0;
        for (int i = 0; i < 10; i++) {
            if (limiter.tryAcquire("u1")) {
                allowed++;
            }
        }
        assertThat(allowed).isEqualTo(3);
    }

    /** 速率与容量至少为 1：配置写错（0 / 负数）时退化为「每 1 秒放行 1 次」，而不是把入口彻底封死。 */
    @Test
    void degenerateConfigStillAllowsOneFromEmptyBucket() {
        RateLimiter limiter = limiter(0, 0);

        assertThat(limiter.tryAcquire("u1")).isTrue();
        assertThat(limiter.tryAcquire("u1")).isFalse();
        advanceMillis(1000);
        assertThat(limiter.tryAcquire("u1")).isTrue();
    }
}
