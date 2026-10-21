package com.orangecrave.admin.system.task;

import com.orangecrave.admin.common.lock.DistributedLock;
import com.orangecrave.admin.system.mapper.SysIdempotencyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 幂等记录 TTL 清理。断言两件事：按保留期算出的截止时间传给删除语句；
 * 拿不到分布式锁时直接跳过（多实例下只有一个节点执行）。不依赖数据库。
 */
class IdempotencyCleanupTaskTest {

    private SysIdempotencyMapper mapper;
    private DistributedLock lock;
    private IdempotencyCleanupTask task;

    @BeforeEach
    void setUp() {
        mapper = Mockito.mock(SysIdempotencyMapper.class);
        lock = Mockito.mock(DistributedLock.class);
        task = new IdempotencyCleanupTask(mapper, lock);
        ReflectionTestUtils.setField(task, "retentionHours", 24L);
    }

    @Test
    void deletesRecordsOlderThanRetention() {
        when(lock.tryLock(anyString(), any())).thenReturn(true);
        when(mapper.deleteOlderThan(any())).thenReturn(3);

        task.cleanup();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).deleteOlderThan(cutoff.capture());
        assertThat(cutoff.getValue()).isAfter(LocalDateTime.now().minusHours(25));
        assertThat(cutoff.getValue()).isBefore(LocalDateTime.now().minusHours(23));
        verify(lock).unlock(anyString());
    }

    @Test
    void skipsWhenLockNotAcquired() {
        when(lock.tryLock(anyString(), any())).thenReturn(false);

        task.cleanup();

        verify(mapper, never()).deleteOlderThan(any());
        verify(lock, never()).unlock(anyString());
    }
}
