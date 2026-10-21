package com.orangecrave.admin.system.task;

import com.orangecrave.admin.common.lock.DistributedLock;
import com.orangecrave.admin.system.mapper.SysIdempotencyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 幂等记录 TTL 清理（见 02 篇 §1.5）。{@code sys_idempotency} 每笔写操作落一行，
 * 不清就是把「表无限增长」写进架构：先按 create_time 建索引，再定期物理删除超期行。
 *
 * <p>保留期默认 24 小时——超过它的重放窗口本就应失效（客户端不该隔天重发同一个键）；
 * 卡在「处理中」的行也一并清除，避免同键被永久占死。多实例下用分布式锁保证只有一个节点执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupTask {

    private static final String LOCK_KEY = "idempotency-cleanup";

    private final SysIdempotencyMapper idempotencyMapper;
    private final DistributedLock distributedLock;

    /** 记录保留小时数。*/
    @Value("${orangecrave.idempotency.retention-hours:24}")
    private long retentionHours;

    @Scheduled(fixedDelayString = "${orangecrave.idempotency.cleanup-interval-ms:3600000}", initialDelay = 300000)
    public void cleanup() {
        if (!distributedLock.tryLock(LOCK_KEY, Duration.ofMinutes(30))) {
            return;
        }
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);
            int removed = idempotencyMapper.deleteOlderThan(cutoff);
            if (removed > 0) {
                log.info("幂等记录清理：删除 {} 条早于 {} 的记录", removed, cutoff);
            }
        } catch (Exception e) {
            log.warn("幂等记录清理失败: {}", e.getMessage());
        } finally {
            distributedLock.unlock(LOCK_KEY);
        }
    }
}
