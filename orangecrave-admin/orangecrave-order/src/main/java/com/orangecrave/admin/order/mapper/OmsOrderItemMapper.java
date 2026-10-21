package com.orangecrave.admin.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orangecrave.admin.order.entity.OmsOrderItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OmsOrderItemMapper extends BaseMapper<OmsOrderItem> {

    /**
     * 某用户在某活动下已占用的件数，用于秒杀每人限购（04 篇 §3.3）。
     * 已取消 9 / 已退款 11 / 非法取消 13 不占额度。 */
    @Select("SELECT COALESCE(SUM(i.qty), 0) FROM oms_order_item i JOIN oms_order o ON o.id = i.order_id "
            + "WHERE i.deleted = 0 AND o.deleted = 0 AND o.activity_id = #{activityId} AND o.user_id = #{userId} "
            + "AND o.status NOT IN (9, 11, 13)")
    int sumQtyByUserAndActivity(@Param("activityId") Long activityId, @Param("userId") Long userId);
}
