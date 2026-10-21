package com.orangecrave.admin.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orangecrave.admin.marketing.entity.MktActivity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface MktActivityMapper extends BaseMapper<MktActivity> {

    /**
     * 秒杀独立库存扣减（04 篇 §3.3）：配额判断与累加在同一条条件 UPDATE 里完成，
     * 影响行数 == 0 即代表被别人抢完，不会出现超卖。
     * total_stock 为 NULL 表示该活动未配额，不扣（下单侧先判）。 */
    @Update("UPDATE mkt_activity SET sold_stock = sold_stock + #{qty}, update_time = NOW() "
            + "WHERE id = #{id} AND deleted = 0 AND total_stock IS NOT NULL "
            + "AND sold_stock + #{qty} <= total_stock")
    int deductSeckillStock(@Param("id") Long id, @Param("qty") int qty);

    /** 归还秒杀配额（取消 / 退款）：只还不扣成负数。 */
    @Update("UPDATE mkt_activity SET sold_stock = CASE WHEN sold_stock > #{qty} THEN sold_stock - #{qty} ELSE 0 END, "
            + "update_time = NOW() WHERE id = #{id} AND deleted = 0 AND sold_stock > 0")
    int restoreSeckillStock(@Param("id") Long id, @Param("qty") int qty);
}
