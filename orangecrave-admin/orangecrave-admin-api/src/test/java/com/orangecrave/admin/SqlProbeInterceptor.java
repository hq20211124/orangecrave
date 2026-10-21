package com.orangecrave.admin;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 零侵入 SQL 探针（仅测试用）。拦截 MyBatis Executor，记录每条 SQL 的墙钟窗口、
 * 累计执行耗时、以及「并发峰值」——即同一时刻有多少条 SQL 在飞。
 *
 * <p>关键度量：{@code peakConcurrent}。若并行化生效，一次 overview() 调用里 4 条查询应
 * 同时进入拦截器（peak>=2），而不再是串行（peak==1）。这是代码层面并行的铁证，
 * 不受 H2/MySQL 真实执行时序影响。
 *
 * <p>{@code perf.rtt.ms} 系统属性给每条 SQL 注入模拟网络往返，使 H2 内存库也能复现
 * 生产中「Σ(RTT) vs max(RTT)」的延迟差异。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SqlProbeInterceptor implements Interceptor {

    public static final AtomicLong SQL_COUNT = new AtomicLong();
    public static final AtomicLong BUSY_NANOS = new AtomicLong();
    public static final AtomicInteger IN_FLIGHT = new AtomicInteger();
    public static final AtomicInteger PEAK = new AtomicInteger();
    private static final AtomicLong WALL_FIRST = new AtomicLong(Long.MAX_VALUE);
    private static final AtomicLong WALL_LAST = new AtomicLong(0);

    public static long rttMs() {
        try {
            return Long.parseLong(System.getProperty("perf.rtt.ms", "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    public static void reset() {
        SQL_COUNT.set(0);
        BUSY_NANOS.set(0);
        IN_FLIGHT.set(0);
        PEAK.set(0);
        WALL_FIRST.set(Long.MAX_VALUE);
        WALL_LAST.set(0);
    }

    public static Snapshot snapshot() {
        long first = WALL_FIRST.get();
        long last = WALL_LAST.get();
        long wall = first == Long.MAX_VALUE ? 0 : (last - first);
        return new Snapshot(SQL_COUNT.get(), wall, BUSY_NANOS.get(), PEAK.get());
    }

    public static class Snapshot {
        public final long sqlCount;
        public final long wallNanos;
        public final long busyNanos;
        public final int peak;

        Snapshot(long c, long w, long b, int p) {
            sqlCount = c;
            wallNanos = w;
            busyNanos = b;
            peak = p;
        }

        public long wallMs() {
            return wallNanos / 1_000_000;
        }

        public long busyMs() {
            return busyNanos / 1_000_000;
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.nanoTime();
        long f;
        do {
            f = WALL_FIRST.get();
        } while (f > start && !WALL_FIRST.compareAndSet(f, start));

        int inflight = IN_FLIGHT.incrementAndGet();
        int p;
        do {
            p = PEAK.get();
        } while (inflight > p && !PEAK.compareAndSet(p, inflight));

        long rtt = rttMs();
        if (rtt > 0) {
            Thread.sleep(rtt);
        }
        try {
            return invocation.proceed();
        } finally {
            IN_FLIGHT.decrementAndGet();
            long end = System.nanoTime();
            BUSY_NANOS.addAndGet(end - start);
            long l;
            do {
                l = WALL_LAST.get();
            } while (end > l && !WALL_LAST.compareAndSet(l, end));
            SQL_COUNT.incrementAndGet();
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
