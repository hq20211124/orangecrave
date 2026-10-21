package com.orangecrave.admin.common.concurrent;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * I/O 并行线程池。供 Service 层把「无依赖的多次数据库/外部查询」并行化，
 * 把单请求延迟从 Σ(RTT) 压到 max(RTT)。
 *
 * <p>规模对齐 HikariCP 连接池（{@code maximum-pool-size=40}）：并行任务各自从连接池取独立连接，
 * 线程数必须 >= 预期并发连接数，否则「线程在等连接」会让并行退化成串行，白做。
 *
 * <p>{@code queueCapacity=0} + {@code CallerRunsPolicy}：需要线程时直接起新线程（max 才生效），
 * 极端过载时由提交线程兜底执行，绝不丢任务。
 */
@Configuration
public class IoConcurrencyConfig {

    public static final String IO_EXECUTOR = "ioExecutor";

    @Bean(IO_EXECUTOR)
    public Executor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(40);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("io-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
