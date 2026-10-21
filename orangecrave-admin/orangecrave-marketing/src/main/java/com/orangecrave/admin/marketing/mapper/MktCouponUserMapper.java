package com.orangecrave.admin.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orangecrave.admin.marketing.entity.MktCouponUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface MktCouponUserMapper extends BaseMapper<MktCouponUser> {

    /** 券到期：只把「未使用」的置为「已过期」，已使用的不动（00 篇 §5「优惠券到期 → 失效」）。 */
    @Update("UPDATE mkt_coupon_user SET status = 3, update_time = NOW() "
            + "WHERE id = #{id} AND status = 1 AND deleted = 0")
    int markExpired(@Param("id") Long id);

    /** 兜底扫描：仍未使用且已过期的券。 */
    @Select("SELECT id FROM mkt_coupon_user WHERE status = 1 AND expire_time IS NOT NULL "
            + "AND expire_time < NOW() AND deleted = 0")
    List<Long> selectExpiredIds();
}
