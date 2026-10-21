package com.orangecrave.admin.common.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 跨实例分布式锁：{@code SET key token NX PX lease} 抢锁、令牌校验后删除释放。
 *
 * <p>两点是「能上多实例」的关键：
 * ① 值存持有者随机令牌而不是固定值——释放走 Lua 脚本「比对令牌再删」，避免 A 的锁超时后
 * 被 B 抢到、A 结束时又把 B 的锁删掉；② 带租约（PX），持锁进程崩溃后锁自动过期，不会死锁。
 *
 * <p>锁记录在 {@link ThreadLocal} 里按 key 保存，同一线程可持多把锁且能各自安全释放。
 * 计时类兜底任务用毫秒级精度做「同一时刻只有一个节点扫」，不需要 Redisson 的可重入/看门狗。
 */
public class RedisDistributedLock implements DistributedLock {

    /** 只有值等于自己写入的令牌时才删除，防止误删他人锁。*/
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    /** 统一命名空间，避免和会话、业务缓存键串。*/
    private final String keyPrefix;
    private final ThreadLocal<Map<String, String>> heldTokens = ThreadLocal.withInitial(HashMap::new);

    public RedisDistributedLock(StringRedisTemplate redis, String keyPrefix) {
        this.redis = redis;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public boolean tryLock(String key, Duration lease) {
        String redisKey = keyPrefix + key;
        String token = UUID.randomUUID().toString();
        Boolean acquired = redis.opsForValue().setIfAbsent(redisKey, token, lease);
        if (Boolean.TRUE.equals(acquired)) {
            heldTokens.get().put(key, token);
            return true;
        }
        return false;
    }

    @Override
    public void unlock(String key) {
        String token = heldTokens.get().remove(key);
        if (token == null) {
            return;
        }
        redis.execute(UNLOCK_SCRIPT, Collections.singletonList(keyPrefix + key), token);
    }
}
