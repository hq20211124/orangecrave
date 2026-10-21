package com.orangecrave.admin.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orangecrave.admin.marketing.entity.MktGroup;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface MktGroupMapper extends BaseMapper<MktGroup> {

    /**
     * 团状态条件 UPDATE（03 篇 §7.1）：满员判定与 24h 到期任务可能同时跑，
     * 影响行数 == 1 的那一方才算抢到收口权。 */
    @Update("UPDATE mkt_group SET status = #{to}, update_time = NOW() "
            + "WHERE id = #{id} AND status = #{from} AND deleted = 0")
    int transition(@Param("id") Long id, @Param("from") int from, @Param("to") int to);

    /** 兜底扫描：仍在拼团中且已到期的团。 */
    @Select("SELECT id FROM mkt_group WHERE status = 1 AND expire_time IS NOT NULL "
            + "AND expire_time < NOW() AND deleted = 0")
    List<Long> selectExpiredIds();
}
