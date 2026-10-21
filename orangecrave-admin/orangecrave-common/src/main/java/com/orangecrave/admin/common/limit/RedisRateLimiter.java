package com.orangecrave.admin.common.limit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

/**
 * Redis 令牌桶：桶状态放在 Redis，多实例共享同一份配额。
 *
 * <p>时间由调用方传入（而非在 Lua 里取 TIME）：Redis 5 之后带写操作的脚本会复制给副本，
 * 脚本里读系统时间会让主从产生不一致，事务式地把时间当参数传才是推荐做法。
 *
 * <p>键按「策略 + 身份」拆分，所以每个身份的桶相互独立；桶闲置一个补满周期后自动过期，
 * 不依赖清理任务。
 */
public class RedisRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "orangecrave:rate:";

    /** ARGV: [rate, burst, nowMillis]；返回 1 放行 / 0 拒绝。 */
    private static final RedisScript<Long> ACQUIRE = new DefaultRedisScript<>(
            "local rate = tonumber(ARGV[1]) "
                    + "local burst = tonumber(ARGV[2]) "
                    + "local now = tonumber(ARGV[3]) "
                    + "local data = redis.call('hmget', KEYS[1], 'tokens', 'ts') "
                    + "local tokens = tonumber(data[1]) "
                    + "local ts = tonumber(data[2]) "
                    + "if tokens == nil then tokens = burst ts = now end "
                    + "local delta = math.max(0, now - ts) / 1000 "
                    + "tokens = math.min(burst, tokens + delta * rate) "
                    + "local ttl = math.ceil(burst / rate * 1000) + 1000 "
                    + "if tokens >= 1 then "
                    + "redis.call('hmset', KEYS[1], 'tokens', tokens - 1, 'ts', now) "
                    + "redis.call('pexpire', KEYS[1], ttl) return 1 "
                    + "end "
                    + "redis.call('hmset', KEYS[1], 'tokens', tokens, 'ts', ts) "
                    + "redis.call('pexpire', KEYS[1], ttl) return 0",
            Long.class);

    private final StringRedisTemplate redis;
    private final int permitsPerSecond;
    private final int burst;

    public RedisRateLimiter(StringRedisTemplate redis, int permitsPerSecond, int burst) {
        this.redis = redis;
        this.permitsPerSecond = Math.max(permitsPerSecond, 1);
        this.burst = Math.max(burst, 1);
    }

    @Override
    public boolean tryAcquire(String key) {
        Long result = redis.execute(ACQUIRE, List.of(KEY_PREFIX + key),
                String.valueOf(permitsPerSecond), String.valueOf(burst),
                String.valueOf(System.currentTimeMillis()));
        return result != null && result == 1L;
    }
}
