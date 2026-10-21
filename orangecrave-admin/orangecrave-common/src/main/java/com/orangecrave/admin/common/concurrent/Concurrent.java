package com.orangecrave.admin.common.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * 并行 I/O 辅助。
 *
 * <p>把「彼此无依赖的多次查询」从串行改为并行，从而把单请求延迟从 Σ(RTT) 压到 max(RTT)。
 * 这是本项目最被忽略的算力来源：此前所有 Service 层 I/O 都是同步串行的，每个瞬间只用 1 个
 * 连接、1 个核，其余核和连接全在等 RTT——这正是「资源空闲但慢」的根因，而不是容量不够。
 *
 * <p>关键约束：并行查询必须位于 {@code @Transactional} 之外，否则会抢同一连接而被串行化。
 * 这些查询各自从连接池取独立连接，因此并行度上限由连接池大小决定，线程池规模须与之对齐。
 */
public final class Concurrent {

    private Concurrent() {
    }

    public static <T> CompletableFuture<T> supply(Executor executor, Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    /**
     * 取结果，并把 {@link CompletableFuture} 抛出的 {@link CompletionException} 拆回原始
     * 运行时异常，保持与同步调用完全一致的错误语义（全局异常处理器行为不变）。
     */
    public static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException ce) {
            Throwable cause = ce.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(cause);
        }
    }
}
