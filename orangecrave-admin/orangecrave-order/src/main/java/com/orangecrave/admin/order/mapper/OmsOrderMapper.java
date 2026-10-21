package com.orangecrave.admin.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orangecrave.admin.order.entity.OmsOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface OmsOrderMapper extends BaseMapper<OmsOrder> {

    /**
     * 带状态守卫的条件 UPDATE（见 00 篇 §5.4 / 03 篇 §2.4）：影响行数 == 1 才说明本次调用赢得了竞争。      */
    @Update("UPDATE oms_order SET status = #{to}, update_time = NOW() "
            + "WHERE id = #{id} AND status = #{from} AND deleted = 0")
    int transition(@Param("id") Long id, @Param("from") int from, @Param("to") int to);

    /** 兜底扫描：某状态下已过期的订单 id。*/
    @Select("SELECT id FROM oms_order WHERE status = #{status} AND expire_time IS NOT NULL "
            + "AND expire_time < NOW() AND deleted = 0")
    List<Long> selectExpiredIds(@Param("status") int status);

    /** 兜底扫描：已发货且在 cutoff 之前就已发货、至今未确认收货的订单 id。
     * 截止时间由 Java 侧算好传入（DATE_SUB + INTERVAL 是 MySQL 方言，H2 跑不了）。*/
    @Select("SELECT o.id FROM oms_order o JOIN oms_delivery d ON d.order_id = o.id AND d.deleted = 0 "
            + "WHERE o.status = #{status} AND o.deleted = 0 "
            + "AND d.delivery_time < #{cutoff}")
    List<Long> selectDeliveredBefore(@Param("status") int status, @Param("cutoff") LocalDateTime cutoff);

    /**
     * 兜底扫描：待发货且在 cutoff 之前就没有动静的订单（03 篇 §2.5「仅告警」）。
     * 进待发货的时间没有独立列，用状态流转时刷新的 update_time 代替。 */
    @Select("SELECT id FROM oms_order WHERE status = #{status} AND deleted = 0 "
            + "AND update_time < #{cutoff}")
    List<Long> selectPendingDeliveryBefore(@Param("status") int status, @Param("cutoff") LocalDateTime cutoff);
}
