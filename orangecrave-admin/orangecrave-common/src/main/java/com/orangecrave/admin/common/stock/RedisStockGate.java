package com.orangecrave.admin.common.stock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis 预扣闸门（见 04 篇 §3.3）。扣减走单条 Lua：读配额、判余量、扣数三件事在 Redis 里
 * 一次原子完成，不像「先 GET 再 DECR」那样在两步之间被其他实例插队，因此不会超卖。
 *
 * <p>配额键带 TTL：活动结束后键自然消失，不必依赖清理任务。判余量失败时不写键，
 * 只在上次预扣后的首次调用里把 total 写进去（惰性初始化，避免活动创建时就要同步一次 Redis）。
 */
@Component
@ConditionalOnProperty(prefix = "orangecrave.redis", name = "enabled", havingValue = "true")
public class RedisStockGate implements StockGate {

    private static final String KEY_PREFIX = "orangecrave:stock:";

    /** ARGV: [total, qty, ttlSeconds]；返回 1 预扣成功 / 0 已抢完。 */
    private static final RedisScript<Long> DEDUCT = new DefaultRedisScript<>(
            "if redis.call('exists', KEYS[1]) == 0 then "
                    + "redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[3]) "
                    + "end "
                    + "if tonumber(redis.call('get', KEYS[1])) >= tonumber(ARGV[2]) then "
                    + "redis.call('decrby', KEYS[1], ARGV[2]) return 1 "
                    + "end "
                    + "return 0",
            Long.class);

    /** 键已过期（活动结束）时归还无意义，直接跳过，避免凭空造出一份配额。 */
    private static final RedisScript<Long> RESTORE = new DefaultRedisScript<>(
            "if redis.call('exists', KEYS[1]) == 1 then return redis.call('incrby', KEYS[1], ARGV[1]) end "
                    + "return 0",
            Long.class);

    private final StringRedisTemplate redis;
    private final long ttlSeconds;

    public RedisStockGate(StringRedisTemplate redis, @Value("${orangecrave.stock.ttl-hours:24}") long ttlHours) {
        this.redis = redis;
        this.ttlSeconds = ttlHours * 3600L;
    }

    @Override
    public boolean tryDeduct(String key, int total, int qty) {
        // 结果类型是 Long，Redis 整数应答由驱动直接返回，不经过值序列化器
        Long result = redis.execute(DEDUCT, List.of(KEY_PREFIX + key),
                String.valueOf(total), String.valueOf(qty), String.valueOf(ttlSeconds));
        return result != null && result == 1L;
    }

    @Override
    public void restore(String key, int qty) {
        redis.execute(RESTORE, List.of(KEY_PREFIX + key), String.valueOf(qty));
    }
}
