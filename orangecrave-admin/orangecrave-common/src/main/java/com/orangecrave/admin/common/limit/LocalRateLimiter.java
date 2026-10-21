package com.orangecrave.admin.common.limit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

/**
 * 进程内令牌桶。以懒补充实现：只在取令牌时按「距上次取令牌过了多久」补足，
 * 无需后台线程定时往里加。Clock 可注入，便于用可控时间做确定性测试。
 *
 * <p>桶按身份（用户 / IP）分片，故桶的数量随活跃身份增长；超过上限整表清空——
 * 限流是尽力而为的准入控制，清空只会让受影响的请求多拿一次突发额度，不会出错。
 */
public class LocalRateLimiter implements RateLimiter {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final int MAX_BUCKETS = 10_000;

    private final int permitsPerSecond;
    private final int burst;
    private final LongSupplier nanoClock;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public LocalRateLimiter(int permitsPerSecond, int burst) {
        this(permitsPerSecond, burst, System::nanoTime);
    }

    /** 测试用：传入可控时钟。 */
    LocalRateLimiter(int permitsPerSecond, int burst, LongSupplier nanoClock) {
        this.permitsPerSecond = Math.max(permitsPerSecond, 1);
        this.burst = Math.max(burst, 1);
        this.nanoClock = nanoClock;
    }

    @Override
    public boolean tryAcquire(String key) {
        if (buckets.size() > MAX_BUCKETS) {
            buckets.clear();
        }
        Bucket bucket = buckets.computeIfAbsent(key, k -> {
            Bucket fresh = new Bucket();
            fresh.tokens = burst;
            fresh.nanos = nanoClock.getAsLong();
            return fresh;
        });
        synchronized (bucket) {
            long now = nanoClock.getAsLong();
            double elapsedSeconds = Math.max(0, now - bucket.nanos) / (double) NANOS_PER_SECOND;
            bucket.tokens = Math.min(burst, bucket.tokens + elapsedSeconds * permitsPerSecond);
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                bucket.nanos = now;
                return true;
            }
            return false;
        }
    }

    /** 令牌数与上次取令牌的时刻。只在 synchronized 块里读写。 */
    private static final class Bucket {
        private double tokens;
        private long nanos;
    }
}
