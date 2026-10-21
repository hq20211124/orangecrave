package com.orangecrave.admin.common.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单进程锁：仅保证同一 JVM 内同名任务不并发/重入，多实例下不起作用。
 * 因此只有未开启 Redis 时才是它兜底；开启 Redis 后由带
 * {@code orangecrave.redis.enabled=true} 的实现接管，本类不再装配。
 */
@Component
@ConditionalOnProperty(prefix = "orangecrave.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalDistributedLock implements DistributedLock {

    private final Set<String> held = ConcurrentHashMap.newKeySet();

    @Override
    public boolean tryLock(String key, Duration lease) {
        // 单进程内无需租约：进程没了锁自然就没了
        return held.add(key);
    }

    @Override
    public void unlock(String key) {
        held.remove(key);
    }
}
