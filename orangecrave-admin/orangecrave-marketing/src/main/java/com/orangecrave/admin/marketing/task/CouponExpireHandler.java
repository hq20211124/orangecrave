package com.orangecrave.admin.marketing.task;

import com.orangecrave.admin.marketing.mapper.MktCouponUserMapper;
import com.orangecrave.admin.marketing.service.MktCouponService;
import com.orangecrave.admin.task.handler.DelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 优惠券到期（00 篇 §5）：把「未使用且已过期」的持券记录置为「已过期」。
 * 已使用/已返还的券由条件 UPDATE 挡住，不会被改状态。 */
@Component
@RequiredArgsConstructor
public class CouponExpireHandler implements DelayTaskHandler {

    public static final String TASK_COUPON_EXPIRE = "COUPON_EXPIRE";

    private final MktCouponUserMapper couponUserMapper;
    private final MktCouponService couponService;

    @Override
    public String taskType() {
        return TASK_COUPON_EXPIRE;
    }

    @Override
    public void execute(Long bizId) {
        couponService.expire(bizId);
    }

    @Override
    public List<Long> scanOverdue() {
        return couponUserMapper.selectExpiredIds();
    }
}
