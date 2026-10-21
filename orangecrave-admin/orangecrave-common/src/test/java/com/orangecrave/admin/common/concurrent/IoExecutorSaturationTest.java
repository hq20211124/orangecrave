package com.orangecrave.admin.common.concurrent;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ioExecutor 过载降级验证。限流把入口量压下来之后，这里要确认的是「万一压不住」时的行为：
 * 池满且无排队空间时，任务由提交线程兜底执行，而不是抛拒绝异常把任务丢掉——
 * 这是 {@code queueCapacity=0} + {@code CallerRunsPolicy} 组合的契约，也是并行查询不丢结果的最后一道保险。
 */
class IoExecutorSaturationTest {

    /** 池上限，与 IoConcurrencyConfig 的 maxPoolSize 一致。 */
    private static final int MAX_POOL_SIZE = 40;

    @Test
    void overflowRunsInCallerThreadInsteadOfBeingRejected() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new IoConcurrencyConfig().ioExecutor();
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch occupied = new CountDownLatch(MAX_POOL_SIZE);
        try {
            // 占满全部线程：每个任务阻塞在 release 上，且队列容量为 0，池里再无接收者
            for (int i = 0; i < MAX_POOL_SIZE; i++) {
                executor.execute(() -> {
                    occupied.countDown();
                    await(release);
                });
            }
            assertThat(occupied.await(5, TimeUnit.SECONDS)).isTrue();

            AtomicBoolean ran = new AtomicBoolean();
            AtomicReference<String> threadName = new AtomicReference<>();
            executor.execute(() -> {
                ran.set(true);
                threadName.set(Thread.currentThread().getName());
            });

            // 池内线程全被占住时提交即在本线程执行完：说明走了 CallerRuns，而不是进队列等别人
            assertThat(ran.get()).isTrue();
            assertThat(threadName.get()).doesNotStartWith("io-");
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
