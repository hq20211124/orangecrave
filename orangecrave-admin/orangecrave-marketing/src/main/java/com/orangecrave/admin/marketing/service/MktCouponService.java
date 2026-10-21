package com.orangecrave.admin.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.marketing.entity.MktCoupon;
import com.orangecrave.admin.marketing.entity.MktCouponUser;
import com.orangecrave.admin.marketing.mapper.MktCouponMapper;
import com.orangecrave.admin.marketing.mapper.MktCouponUserMapper;
import com.orangecrave.admin.marketing.model.dto.MktCouponSaveDTO;
import com.orangecrave.admin.marketing.task.CouponExpireHandler;
import com.orangecrave.admin.task.engine.DelayTaskEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 优惠券（见 04 篇 §3.4）。领取中心属顾客端，后台只做发放与核销管理。
 * 拼团价与优惠券互斥（下单侧校验）。 */
@Service
@RequiredArgsConstructor
public class MktCouponService {

    private final MktCouponMapper couponMapper;
    private final MktCouponUserMapper couponUserMapper;
    private final DelayTaskEngine delayTaskEngine;

    public PageResult<MktCoupon> page(PageQuery query) {
        Page<MktCoupon> page = couponMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<MktCoupon>().orderByDesc(MktCoupon::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public void save(MktCouponSaveDTO dto) {
        MktCoupon coupon = new MktCoupon();
        coupon.setId(dto.getId());
        coupon.setActivityId(dto.getActivityId());
        coupon.setCouponName(dto.getCouponName());
        coupon.setCouponType(dto.getCouponType());
        coupon.setThreshold(dto.getThreshold());
        coupon.setAmount(dto.getAmount());
        coupon.setDiscount(dto.getDiscount());
        coupon.setValidDays(dto.getValidDays());
        coupon.setTotalQty(dto.getTotalQty());
        if (coupon.getId() == null) {
            // issued_qty 只由发放动作累加，后台不能直接改
            coupon.setIssuedQty(0);
            couponMapper.insert(coupon);
        } else {
            couponMapper.updateById(coupon);
        }
    }

    public void delete(Long id) {
        couponMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @OperLog(module = "优惠券", type = "UPDATE", desc = "定向发放优惠券")
    public int grant(Long couponId, List<Long> userIds) {
        MktCoupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BizException("券模板不存在");
        }
        int granted = 0;
        for (Long userId : userIds) {
            MktCouponUser userCoupon = new MktCouponUser();
            userCoupon.setCouponId(couponId);
            userCoupon.setUserId(userId);
            userCoupon.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
            userCoupon.setStatus(1);
            int validDays = coupon.getValidDays() == null ? 30 : coupon.getValidDays();
            userCoupon.setExpireTime(LocalDateTime.now().plusDays(validDays));
            userCoupon.setSource(1);
            couponUserMapper.insert(userCoupon);
            delayTaskEngine.schedule(CouponExpireHandler.TASK_COUPON_EXPIRE, userCoupon.getId(),
                    validDays * 24L * 3_600_000L);
            granted++;
        }
        MktCoupon update = new MktCoupon();
        update.setId(couponId);
        update.setIssuedQty((coupon.getIssuedQty() == null ? 0 : coupon.getIssuedQty()) + granted);
        couponMapper.updateById(update);
        return granted;
    }

    public PageResult<MktCouponUser> userPage(PageQuery query, Long userId) {
        Page<MktCouponUser> page = couponUserMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<MktCouponUser>()
                        .eq(userId != null, MktCouponUser::getUserId, userId)
                        .orderByDesc(MktCouponUser::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /** 到期失效（00 篇 §5「优惠券到期 → 失效」）。条件 UPDATE，重复触发只改一次。 */
    public void expire(Long couponUserId) {
        couponUserMapper.markExpired(couponUserId);
    }
}
