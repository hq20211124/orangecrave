package com.orangecrave.admin.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orangecrave.admin.work.entity.WmsShareCode;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

public interface WmsShareCodeMapper extends BaseMapper<WmsShareCode> {

    /**
     * 到期失效（00 篇 §5「分享码到期 → 失效 + 退未核销预付款」）。
     * 只改「有效」状态的行，已强制失效/已退完的不再重复处理。 */
    @Update("UPDATE wms_share_code SET status = 0, invalid_reason = #{reason}, update_time = NOW() "
            + "WHERE id = #{id} AND status = 1 AND deleted = 0")
    int markExpired(@Param("id") Long id, @Param("reason") String reason);

    /** 兜底扫描：仍有效且已过期的分享码。 */
    @Select("SELECT id FROM wms_share_code WHERE status = 1 AND expire_time IS NOT NULL "
            + "AND expire_time < NOW() AND deleted = 0")
    List<Long> selectExpiredIds();

    /**
     * 核销（01 篇 §5⑤ / 03 篇 §7.3）：套数与已核销预付款一起加，
     * 上限判断写在 WHERE 里，超量那次影响 0 行。 */
    @Update("UPDATE wms_share_code SET used_qty = used_qty + #{qty}, prepay_used = prepay_used + #{amount}, "
            + "update_time = NOW() WHERE id = #{id} AND status = 1 AND deleted = 0 "
            + "AND (expire_time IS NULL OR expire_time > NOW()) "
            + "AND (max_order_qty IS NULL OR used_qty + #{qty} <= max_order_qty)")
    int redeem(@Param("id") Long id, @Param("qty") int qty, @Param("amount") BigDecimal amount);
}
