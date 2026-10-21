package com.orangecrave.admin.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orangecrave.admin.work.entity.WmsWork;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface WmsWorkMapper extends BaseMapper<WmsWork> {

    /** 作品到期（00 篇 §5「作品过期 → 状态转作品过期」）：只从「正常」转，已下架/违规的不覆盖。 */
    @Update("UPDATE wms_work SET status = #{to}, update_time = NOW() "
            + "WHERE id = #{id} AND status = #{from} AND deleted = 0")
    int markExpired(@Param("id") Long id, @Param("from") int from, @Param("to") int to);

    /** 兜底扫描：仍正常且已过有效期的作品。 */
    @Select("SELECT id FROM wms_work WHERE status = 1 AND expire_time IS NOT NULL "
            + "AND expire_time < NOW() AND deleted = 0")
    List<Long> selectExpiredIds();
}
