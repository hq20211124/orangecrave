package com.orangecrave.admin;

import com.orangecrave.admin.finance.service.FinDashboardService;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 并行化收益验证。配合 {@link SqlProbeInterceptor} 的模拟 RTT 与并发峰值统计，
 * 直接证明「无依赖查询从串行改为并行」确实生效。
 *
 * <p>核心断言是 {@code peakConcurrent >= 2}：一次调用里有多条 SQL 在同一时刻并发执行，
 * 这不受 H2/MySQL 真实执行时序影响，是代码并行化的铁证。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ParallelismBenchTest {

    @Autowired
    ApplicationContext ctx;

    @BeforeEach
    void setUp() {
        // 探针直接织进共享的 MyBatis Configuration：不能用 @TestConfiguration 注册成 Bean，
        // 那会改变本测试的上下文配置、新建第二个 ApplicationContext；而 H2 内存库
        // DB_CLOSE_DELAY=-1 整个 JVM 只有一份，第二个上下文会重复执行 data.sql 撞主键。
        // 与其余测试类共享同一个上下文，schema/data 只初始化一次。
        org.apache.ibatis.session.Configuration mbConfig =
                ctx.getBean(SqlSessionFactory.class).getConfiguration();
        if (mbConfig.getInterceptors().stream().noneMatch(SqlProbeInterceptor.class::isInstance)) {
            mbConfig.addInterceptor(new SqlProbeInterceptor());
        }
        // 预热：先各跑一遍（无模拟 RTT），让 H2 编译好执行计划、连接池建好连接、JIT 就位，
        // 再清零探针只测正式这一轮——否则首轮冷启动开销会淹没 RTT 信号
        System.setProperty("perf.rtt.ms", "0");
        FinDashboardService svc = ctx.getBean(FinDashboardService.class);
        svc.overview();
        svc.todo();
        SqlProbeInterceptor.reset();
        // 注入 50ms 模拟 RTT：串行 = Σ(RTT) ≈ 200ms+，并行 = max(RTT) ≈ 50ms，
        // 差值拉大到执行噪音无法掩盖，墙钟断言才有判别力
        System.setProperty("perf.rtt.ms", "50");
    }

    @AfterEach
    void tearDown() {
        // RTT 是全局系统属性，清零以免拖慢同 JVM 里后续测试的每条 SQL
        System.setProperty("perf.rtt.ms", "0");
    }

    @Test
    void overview_runs_queries_in_parallel() {
        FinDashboardService dashboardService = ctx.getBean(FinDashboardService.class);
        dashboardService.overview();
        SqlProbeInterceptor.Snapshot s = SqlProbeInterceptor.snapshot();
        System.out.println("[probe] overview -> sql=" + s.sqlCount
                + " wall=" + s.wallMs() + "ms busy=" + s.busyMs() + "ms peakConcurrent=" + s.peak);
        assertEquals(4, s.sqlCount, "overview 应有 4 条独立查询");
        assertTrue(s.peak >= 2, "4 条查询必须并发执行(peak>=2)，否则并行化没生效");
        // 串行下墙钟 >= 4*RTT=200ms；并行应接近 1*RTT。阈值取两者之间
        assertTrue(s.wallMs() < 180, "并行墙钟应≈max(RTT)，实际=" + s.wallMs() + "ms");
    }

    @Test
    void todo_runs_queries_in_parallel() {
        FinDashboardService dashboardService = ctx.getBean(FinDashboardService.class);
        dashboardService.todo();
        SqlProbeInterceptor.Snapshot s = SqlProbeInterceptor.snapshot();
        System.out.println("[probe] todo -> sql=" + s.sqlCount
                + " wall=" + s.wallMs() + "ms busy=" + s.busyMs() + "ms peakConcurrent=" + s.peak);
        // 4 个指标 = 5 条 SQL：超时单量要先取一次阈值配置，再链式查单量（两轮，其余三条并行）
        assertEquals(5, s.sqlCount, "todo 应有 5 条 SQL（4 指标 + 1 次超时阈值配置）");
        assertTrue(s.peak >= 2, "todo 的无依赖查询必须并发执行(peak>=2)");
        // 串行下墙钟 >= 5*RTT=250ms；并行两轮应接近 2*RTT。阈值取两者之间
        assertTrue(s.wallMs() < 230, "并行两轮墙钟应≈2*max(RTT)，实际=" + s.wallMs() + "ms");
    }
}
