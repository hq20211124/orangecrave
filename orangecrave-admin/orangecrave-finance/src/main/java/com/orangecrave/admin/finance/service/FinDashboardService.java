package com.orangecrave.admin.finance.service;

import com.orangecrave.admin.common.concurrent.Concurrent;
import com.orangecrave.admin.common.concurrent.IoConcurrencyConfig;
import com.orangecrave.admin.finance.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 看板（见 06 篇 §5）。分三页：经营看板 / 运营待办 / 财务对账。  */
@Service
@RequiredArgsConstructor
public class FinDashboardService {

    private final DashboardMapper dashboardMapper;

    /** 品类排行只看近 N 天（默认 30）。*/
    @Value("${orangecrave.dashboard.rank-days:30}")
    private int rankDays;

    /** 看板各查询彼此无依赖，并行化把延迟从 Σ(RTT) 压到 max(RTT)。*/
    @Qualifier(IoConcurrencyConfig.IO_EXECUTOR)
    @Autowired
    private Executor ioExecutor;

    /** 经营看板。*/
    public Map<String, Object> overview() {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime rankSince = LocalDate.now().minusDays(rankDays - 1L).atStartOfDay();
        CompletableFuture<BigDecimal> todaySalesF = Concurrent.supply(ioExecutor, dashboardMapper::todaySales);
        CompletableFuture<BigDecimal> monthSalesF =
                Concurrent.supply(ioExecutor, () -> dashboardMapper.monthSales(monthStart));
        CompletableFuture<Integer> todayOrdersF = Concurrent.supply(ioExecutor, dashboardMapper::todayOrderCount);
        CompletableFuture<List<Map<String, Object>>> categoryRankF =
                Concurrent.supply(ioExecutor, () -> dashboardMapper.categoryRank(rankSince));

        BigDecimal todaySales = nz(Concurrent.join(todaySalesF));
        BigDecimal monthSales = nz(Concurrent.join(monthSalesF));
        int todayOrders = Concurrent.join(todayOrdersF);
        BigDecimal avg = todayOrders == 0 ? BigDecimal.ZERO
                : todaySales.divide(BigDecimal.valueOf(todayOrders), 2, RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("todaySales", todaySales);
        result.put("monthSales", monthSales);
        result.put("todayOrderCount", todayOrders);
        result.put("avgOrderAmount", avg);
        result.put("categoryRank", Concurrent.join(categoryRankF));
        return result;
    }

    /** 运营待办（登录后首页数据，客服每天真正要盯的指标）。**/
    public Map<String, Object> todo() {
        CompletableFuture<List<Map<String, Object>>> statusCountsF =
                Concurrent.supply(ioExecutor, dashboardMapper::orderStatusCounts);
        CompletableFuture<Integer> pendingTicketsF = Concurrent.supply(ioExecutor, dashboardMapper::pendingTicketCount);
        CompletableFuture<Integer> failedTasksF = Concurrent.supply(ioExecutor, dashboardMapper::failedTaskCount);
        // 超时单量依赖阈值配置：阈值查询与其余三条并行发出，拿到后再在同一个
        // 线程池上接着算截止时间并查单量，链式保持「无依赖的部分」仍然并行
        CompletableFuture<Integer> overdueF = Concurrent.supply(ioExecutor, dashboardMapper::deliveryAlertHours)
                .thenApplyAsync(hours ->
                        dashboardMapper.overdueDeliveryCount(LocalDateTime.now().minusHours(hours)), ioExecutor);

        Map<Integer, Long> byStatus = new HashMap<>();
        for (Map<String, Object> row : Concurrent.join(statusCountsF)) {
            Integer status = ((Number) row.get("status")).intValue();
            byStatus.put(status, ((Number) row.get("c")).longValue());
        }
        Map<String, Object> todo = new HashMap<>();
        todo.put("unpaidOrders", byStatus.getOrDefault(0, 0L));
        todo.put("pendingAuditOrders", byStatus.getOrDefault(2, 0L));
        todo.put("pendingProductionOrders", byStatus.getOrDefault(3, 0L));
        todo.put("pendingDeliveryOrders", byStatus.getOrDefault(6, 0L));
        todo.put("overdueDeliveryOrders", (long) Concurrent.join(overdueF));
        todo.put("pendingTickets", (long) Concurrent.join(pendingTicketsF));
        todo.put("failedTasks", (long) Concurrent.join(failedTasksF));
        return todo;
    }

    public List<Map<String, Object>> trend() {
        return dashboardMapper.trend(LocalDate.now().minusDays(13).atStartOfDay());
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
