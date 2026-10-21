package com.orangecrave.admin.finance.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 看板/对账只读查询。直接读 oms_ 等表，**不依赖 order 模块**（避免循环依赖，见 finance pom 说明）。  */
public interface DashboardMapper {

    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM oms_order "
            + "WHERE deleted = 0 AND order_type <> 2 AND pay_time IS NOT NULL AND pay_time >= CURDATE()")
    BigDecimal todaySales();

    /** 本月销售额。月起始日由 Java 侧算好传入（DATE_FORMAT 是 MySQL 方言，H2 跑不了）。*/
    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM oms_order "
            + "WHERE deleted = 0 AND order_type <> 2 AND pay_time IS NOT NULL AND pay_time >= #{monthStart}")
    BigDecimal monthSales(@Param("monthStart") LocalDateTime monthStart);

    @Select("SELECT COUNT(*) FROM oms_order "
            + "WHERE deleted = 0 AND order_type <> 2 AND pay_time IS NOT NULL AND pay_time >= CURDATE()")
    int todayOrderCount();

    @Select("SELECT status, COUNT(*) AS c FROM oms_order WHERE deleted = 0 GROUP BY status")
    List<Map<String, Object>> orderStatusCounts();

    @Select("SELECT COUNT(*) FROM svc_ticket WHERE deleted = 0 AND status IN (1, 2)")
    int pendingTicketCount();

    @Select("SELECT COUNT(*) FROM tsk_delay_log WHERE deleted = 0 AND status = 3")
    int failedTaskCount();

    /**
     * 待发货超时阈值（小时）：sys_config pendingDeliveryHours，缺省 72。
     * 与 ORDER_DELIVERY_ALERT 用同一个系统参数（00 篇 §5「超时只告警」）。 */
    @Select("SELECT COALESCE(MAX(CAST(c.config_value AS BIGINT)), 72) FROM sys_config c "
            + "WHERE c.config_key = 'pendingDeliveryHours' AND c.deleted = 0")
    long deliveryAlertHours();

    /**
     * 待发货超时单量。截止时间由 Java 侧按 {@link #deliveryAlertHours()} 算好传入
     * （DATE_SUB + INTERVAL 是 MySQL 方言，H2 跑不了）。 */
    @Select("SELECT COUNT(*) FROM oms_order o WHERE o.deleted = 0 AND o.status = 6 "
            + "AND o.update_time < #{cutoff}")
    int overdueDeliveryCount(@Param("cutoff") LocalDateTime cutoff);

    /** 近 14 天销售趋势（支付口径）。起始日由 Java 侧算好传入；按支付日期分组，
     * CAST(... AS DATE) 在 MySQL/H2 下都可用，序列化后与原来的 DATE_FORMAT 输出同为 yyyy-MM-dd。*/
    @Select("SELECT CAST(pay_time AS DATE) AS d, COALESCE(SUM(pay_amount), 0) AS amount "
            + "FROM oms_order WHERE deleted = 0 AND pay_time IS NOT NULL "
            + "AND pay_time >= #{since} "
            + "GROUP BY CAST(pay_time AS DATE) ORDER BY d")
    List<Map<String, Object>> trend(@Param("since") LocalDateTime since);

    /** 品类销售排行（近 N 天，起始日由 Java 侧算好传入）。只看近期，避免存量增长后
     * 变成一次永久全表聚合。*/
    @Select("SELECT c.category_name AS name, COALESCE(SUM(i.total_price), 0) AS amount "
            + "FROM oms_order_item i "
            + "JOIN oms_order o ON o.id = i.order_id AND o.deleted = 0 "
            + "JOIN pms_spu s ON s.id = i.spu_id AND s.deleted = 0 "
            + "JOIN pms_category c ON c.id = s.category_id AND c.deleted = 0 "
            + "WHERE i.deleted = 0 AND o.pay_time IS NOT NULL AND o.pay_time >= #{since} "
            + "GROUP BY c.category_name ORDER BY amount DESC LIMIT 10")
    List<Map<String, Object>> categoryRank(@Param("since") LocalDateTime since);
}
