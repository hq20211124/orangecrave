package com.orangecrave.admin.marketing.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.stock.StockGate;
import com.orangecrave.admin.goods.entity.PmsSku;
import com.orangecrave.admin.goods.mapper.PmsSkuMapper;
import com.orangecrave.admin.marketing.entity.MktActivity;
import com.orangecrave.admin.marketing.entity.MktCoupon;
import com.orangecrave.admin.marketing.entity.MktCouponUser;
import com.orangecrave.admin.marketing.mapper.MktActivityMapper;
import com.orangecrave.admin.marketing.mapper.MktCouponMapper;
import com.orangecrave.admin.marketing.mapper.MktCouponUserMapper;
import com.orangecrave.admin.order.adapter.MarketingAdapter;
import com.orangecrave.admin.order.mapper.OmsOrderItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 营销让利实现（04 篇 §3.2 / §3.4 / §4）。让利口径都收在这一个类里，
 * 订单侧只管按 §4 的顺序编排，不关心活动与券的规则细节。 */
@Service
@RequiredArgsConstructor
public class MarketingAdapterImpl implements MarketingAdapter {

    private static final int TYPE_GROUP = 1;
    private static final int TYPE_SECKILL = 2;
    private static final int MONEY_SCALE = 2;

    private final MktActivityMapper activityMapper;
    private final MktCouponMapper couponMapper;
    private final MktCouponUserMapper couponUserMapper;
    private final PmsSkuMapper skuMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final ObjectMapper objectMapper;
    /** 秒杀配额的内存闸门：Redis 开优先挡在库前，关则一律放行（见 StockGate）。 */
    private final StockGate stockGate;

    @Override
    public BigDecimal activityUnitPrice(Long activityId, Long skuId) {
        if (activityId == null || skuId == null) {
            return null;
        }
        // SPU 惰性取：活动不存在 / 已停用 / 不在时间窗时直接返回 null，不该为此查一次 SKU
        return unitPrice(activityId, skuId, () -> {
            PmsSku sku = skuMapper.selectById(skuId);
            if (sku == null) {
                throw new BizException("SKU 不存在");
            }
            return sku.getSpuId();
        });
    }

    /** 调用方已查出 SKU 时走这里：不再回查 SKU，只查活动本身。 */
    @Override
    public BigDecimal activityUnitPrice(Long activityId, Long skuId, Long spuId) {
        if (activityId == null || skuId == null) {
            return null;
        }
        return unitPrice(activityId, skuId, () -> spuId);
    }

    private BigDecimal unitPrice(Long activityId, Long skuId, Supplier<Long> spuIdLoader) {
        MktActivity activity = activityMapper.selectById(activityId);
        if (activity == null || !running(activity)) {
            return null;
        }
        if (!covers(activity, skuId, spuIdLoader.get())) {
            throw new BizException("该商品不参加此活动");
        }
        JsonNode rule = parseRule(activity);
        String field = activity.getActivityType() != null && activity.getActivityType() == TYPE_GROUP
                ? "groupPrice" : "seckillPrice";
        BigDecimal price = rule.path(field).isNumber() || rule.path(field).isTextual()
                ? new BigDecimal(rule.path(field).asText()) : null;
        if (price == null) {
            price = rule.path("activityPrice").isMissingNode() ? null : new BigDecimal(rule.path("activityPrice").asText());
        }
        if (price == null) {
            throw new BizException("活动未配置活动价");
        }
        if (price.signum() < 0) {
            throw new BizException("活动价不合法");
        }
        return price.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal couponDeduction(Long couponUserId, Long userId, BigDecimal itemAmount, boolean hasActivityPrice) {
        if (couponUserId == null) {
            return BigDecimal.ZERO;
        }
        if (hasActivityPrice) {
            // 04 篇 §4 步骤 2：券仅在无活动价时生效
            throw new BizException("活动价与优惠券不可叠加使用");
        }
        MktCouponUser couponUser = requireOwnedUsable(couponUserId, userId);
        MktCoupon template = couponMapper.selectById(couponUser.getCouponId());
        if (template == null) {
            throw new BizException("优惠券已失效");
        }
        int type = template.getCouponType() == null ? 3 : template.getCouponType();
        BigDecimal amount = nvl(template.getAmount());
        BigDecimal deducted;
        switch (type) {
            case 1 -> {
                if (itemAmount.compareTo(nvl(template.getThreshold())) < 0) {
                    throw new BizException("未满 " + template.getThreshold().stripTrailingZeros().toPlainString() + " 元，不能用这张满减券");
                }
                deducted = amount;
            }
            case 2 -> {
                BigDecimal rate = nvl(template.getDiscount());
                if (rate.signum() <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
                    throw new BizException("折扣券配置错误：discount 需为 0~1 的折扣率");
                }
                deducted = itemAmount.multiply(BigDecimal.ONE.subtract(rate)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
            default -> deducted = amount;
        }
        // 抵扣不能超过商品金额（运费不参与让利）
        return deducted.min(itemAmount).max(BigDecimal.ZERO).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public void lockCoupon(Long couponUserId, Long userId, Long orderId) {
        if (couponUserId == null) {
            return;
        }
        requireOwnedUsable(couponUserId, userId);
        int rows = couponUserMapper.update(null, new LambdaUpdateWrapper<MktCouponUser>()
                .eq(MktCouponUser::getId, couponUserId)
                .eq(MktCouponUser::getStatus, 1)
                .isNull(MktCouponUser::getOrderId)
                .set(MktCouponUser::getOrderId, orderId));
        if (rows != 1) {
            throw new BizException("优惠券刚被占用，请重新选择");
        }
    }

    @Override
    public void useCoupon(Long orderId) {
        couponUserMapper.update(null, new LambdaUpdateWrapper<MktCouponUser>()
                .eq(MktCouponUser::getOrderId, orderId)
                .eq(MktCouponUser::getStatus, 1)
                .set(MktCouponUser::getStatus, 2)
                .set(MktCouponUser::getUseTime, LocalDateTime.now()));
    }

    @Override
    public void releaseCoupon(Long orderId) {
        couponUserMapper.update(null, new LambdaUpdateWrapper<MktCouponUser>()
                .eq(MktCouponUser::getOrderId, orderId)
                .in(MktCouponUser::getStatus, 1, 2)
                .set(MktCouponUser::getStatus, 1)
                .set(MktCouponUser::getOrderId, null)
                .set(MktCouponUser::getUseTime, null));
    }

    /**
     * 秒杀下单扣独立库存 + 每人限购（04 篇 §3.3）。
     * 两级扣减：① 先过内存闸门，抢不到的直接在库前失败，数据库只承接赢家；
     * ② 真正的账本是条件 UPDATE，影响 0 行即判抢完——闸门漏掉或状态失真也不会超卖。
     *
     * <p>已知取舍：闸门扣的是 Redis/内存里的计数，不随数据库事务回滚。若本次建单后续步骤失败回滚，
     * 这份预扣会偏悲观（少放行一个），直到配额键过期重建——方向是「少卖」而非「超卖」，
     * 账本始终以数据库为准，故不做猜测性的反向补偿；取消 / 退款由 {@link #releaseSeckillStock} 明确归还。 */
    @Override
    public void reserveSeckillStock(Long activityId, Long userId, int qty) {
        MktActivity activity = seckillOrNull(activityId);
        if (activity == null) {
            return;
        }
        int perUserLimit = ruleInt(activity, "perUserLimit");
        if (perUserLimit > 0 && userId != null) {
            int bought = orderItemMapper.sumQtyByUserAndActivity(activityId, userId);
            if (bought + qty > perUserLimit) {
                throw new BizException("该秒杀每人限购 " + perUserLimit + " 件，你已购 " + bought + " 件");
            }
        }
        if (activity.getTotalStock() == null) {
            return;
        }
        String gateKey = seckillGateKey(activityId);
        if (!stockGate.tryDeduct(gateKey, activity.getTotalStock(), qty)) {
            throw new BizException("秒杀库存已抢完");
        }
        if (activityMapper.deductSeckillStock(activityId, qty) != 1) {
            // 闸门放行但落库不成功（预扣状态失真，或已被并发回滚）→ 必须归还，否则配额凭空少一份
            stockGate.restore(gateKey, qty);
            throw new BizException("秒杀库存已抢完");
        }
    }

    /** 取消 / 退款归还秒杀配额；活动已结束时也要还，否则配额被死单占住。 */
    @Override
    public void releaseSeckillStock(Long activityId, int qty) {
        MktActivity activity = seckillOrNull(activityId);
        if (activity == null || activity.getTotalStock() == null) {
            return;
        }
        activityMapper.restoreSeckillStock(activityId, qty);
        // 内存预扣同步归还，否则配额被取消掉的订单一直占着
        stockGate.restore(seckillGateKey(activityId), qty);
    }

    private String seckillGateKey(Long activityId) {
        return "seckill:" + activityId;
    }

    private MktActivity seckillOrNull(Long activityId) {
        if (activityId == null) {
            return null;
        }
        MktActivity activity = activityMapper.selectById(activityId);
        if (activity == null || activity.getActivityType() == null || activity.getActivityType() != TYPE_SECKILL) {
            return null;
        }
        return activity;
    }

    private int ruleInt(MktActivity activity, String field) {
        JsonNode node = parseRule(activity).path(field);
        return node.isNumber() || node.isTextual() ? node.asInt() : 0;
    }

    private MktCouponUser requireOwnedUsable(Long couponUserId, Long userId) {
        MktCouponUser couponUser = couponUserMapper.selectById(couponUserId);
        if (couponUser == null || userId == null || !userId.equals(couponUser.getUserId())) {
            throw new BizException("优惠券不属于当前用户");
        }
        if (couponUser.getStatus() == null || couponUser.getStatus() != 1) {
            throw new BizException("优惠券已使用或已过期");
        }
        if (couponUser.getOrderId() != null) {
            throw new BizException("优惠券已被其他订单占用");
        }
        if (couponUser.getExpireTime() != null && couponUser.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException("优惠券已过期");
        }
        return couponUser;
    }

    private boolean running(MktActivity activity) {
        if (activity.getStatus() == null || activity.getStatus() != 1) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return (activity.getStartTime() == null || !now.isBefore(activity.getStartTime()))
                && (activity.getEndTime() == null || !now.isAfter(activity.getEndTime()));
    }

    /** 商品适用范围：sku_ids 命中 SKU，或 spu_ids 命中其 SPU；两者都空视为全场通用。 */
    private boolean covers(MktActivity activity, Long skuId, Long spuId) {
        boolean any = hasText(activity.getSkuIds()) || hasText(activity.getSpuIds());
        if (!any) {
            return true;
        }
        return containsId(activity.getSkuIds(), skuId)
                || (spuId != null && containsId(activity.getSpuIds(), spuId));
    }

    private boolean containsId(String json, Long id) {
        if (!hasText(json)) {
            return false;
        }
        try {
            for (JsonNode node : objectMapper.readTree(json)) {
                if (node.asLong() == id) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new BizException("活动商品范围配置错误");
        }
        return false;
    }

    private JsonNode parseRule(MktActivity activity) {
        try {
            return objectMapper.readTree(hasText(activity.getRuleConfig()) ? activity.getRuleConfig() : "{}");
        } catch (Exception e) {
            throw new BizException("活动规则配置不是合法 JSON");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank() && !"null".equals(value);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
