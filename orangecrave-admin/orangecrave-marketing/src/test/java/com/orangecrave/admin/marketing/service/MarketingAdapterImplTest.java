package com.orangecrave.admin.marketing.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.stock.NoopStockGate;
import com.orangecrave.admin.common.stock.StockGate;
import com.orangecrave.admin.goods.entity.PmsSku;
import com.orangecrave.admin.goods.mapper.PmsSkuMapper;
import com.orangecrave.admin.marketing.entity.MktActivity;
import com.orangecrave.admin.marketing.entity.MktCoupon;
import com.orangecrave.admin.marketing.entity.MktCouponUser;
import com.orangecrave.admin.marketing.mapper.MktActivityMapper;
import com.orangecrave.admin.marketing.mapper.MktCouponMapper;
import com.orangecrave.admin.marketing.mapper.MktCouponUserMapper;
import com.orangecrave.admin.order.mapper.OmsOrderItemMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 让利规则测试（04 篇 §3.2 / §3.2.2 / §3.4 / §4）。不依赖数据库。 */
class MarketingAdapterImplTest {

    private MktActivityMapper activityMapper;
    private MktCouponMapper couponMapper;
    private MktCouponUserMapper couponUserMapper;
    private PmsSkuMapper skuMapper;
    private OmsOrderItemMapper orderItemMapper;
    private StockGate stockGate;
    private MarketingAdapterImpl adapter;

    @BeforeEach
    void setUp() {
        // 纯 Mockito 环境里没有 SqlSession，Lambda 列名解析依赖 MyBatis-Plus 的 TableInfo 缓存，需手工注册
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), MktCouponUser.class);
        activityMapper = Mockito.mock(MktActivityMapper.class);
        couponMapper = Mockito.mock(MktCouponMapper.class);
        couponUserMapper = Mockito.mock(MktCouponUserMapper.class);
        skuMapper = Mockito.mock(PmsSkuMapper.class);
        orderItemMapper = Mockito.mock(OmsOrderItemMapper.class);
        // 默认走「无 Redis」闸门：一律放行，把判定完全交给数据库条件 UPDATE（与旧行为一致）
        stockGate = new NoopStockGate();
        adapter = new MarketingAdapterImpl(activityMapper, couponMapper, couponUserMapper,
                skuMapper, orderItemMapper, new ObjectMapper(), stockGate);
    }

    private PmsSku sku(long id, long spuId) {
        PmsSku sku = new PmsSku();
        sku.setId(id);
        sku.setSpuId(spuId);
        return sku;
    }

    private MktActivity activity(long id, int type, String spuIds, String skuIds, String rule, int status) {
        MktActivity activity = new MktActivity();
        activity.setId(id);
        activity.setActivityType(type);
        activity.setSpuIds(spuIds);
        activity.setSkuIds(skuIds);
        activity.setRuleConfig(rule);
        activity.setStatus(status);
        activity.setStartTime(LocalDateTime.now().minusDays(1));
        activity.setEndTime(LocalDateTime.now().plusDays(1));
        return activity;
    }

    private MktActivity seckill(long id, String rule) {
        return activity(id, 2, null, null, rule, 1);
    }

    private MktCouponUser userCoupon(long id, long couponId, long userId, int status, Long orderId) {
        MktCouponUser couponUser = new MktCouponUser();
        couponUser.setId(id);
        couponUser.setCouponId(couponId);
        couponUser.setUserId(userId);
        couponUser.setStatus(status);
        couponUser.setOrderId(orderId);
        couponUser.setExpireTime(LocalDateTime.now().plusDays(30));
        return couponUser;
    }

    @Test
    void activityIdNullMeansOriginalPrice() {
        assertThat(adapter.activityUnitPrice(null, 1L)).isNull();
        verify(activityMapper, never()).selectById(any());
    }

    @Test
    void stoppedActivityYieldsNoActivityPrice() {
        when(activityMapper.selectById(3L)).thenReturn(activity(3, 1, "[1]", null, "{\"groupPrice\":0.90}", 0));
        assertThat(adapter.activityUnitPrice(3L, 1L)).isNull();
    }

    @Test
    void groupActivityReadsGroupPriceAndSeckillReadsSeckillPrice() {
        when(skuMapper.selectById(1L)).thenReturn(sku(1, 7));
        when(activityMapper.selectById(1L)).thenReturn(activity(1, 1, "[7]", null, "{\"groupPrice\":1.50}", 1));
        when(activityMapper.selectById(2L)).thenReturn(activity(2, 2, null, "[1]", "{\"seckillPrice\":1.00}", 1));

        assertThat(adapter.activityUnitPrice(1L, 1L)).isEqualByComparingTo("1.50");
        assertThat(adapter.activityUnitPrice(2L, 1L)).isEqualByComparingTo("1.00");
    }

    @Test
    void activityNotCoveringThisSkuIsRejected() {
        when(skuMapper.selectById(1L)).thenReturn(sku(1, 7));
        when(activityMapper.selectById(1L)).thenReturn(activity(1, 1, "[999]", "[888]", "{\"groupPrice\":1.50}", 1));

        assertThatThrownBy(() -> adapter.activityUnitPrice(1L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不参加此活动");
    }

    @Test
    void activityWithoutPriceConfigIsNotSilentlyPricedAtFullAmount() {
        when(skuMapper.selectById(1L)).thenReturn(sku(1, 7));
        when(activityMapper.selectById(1L)).thenReturn(activity(1, 1, null, null, "{\"groupSize\":3}", 1));

        assertThatThrownBy(() -> adapter.activityUnitPrice(1L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未配置活动价");
    }

    @Test
    void noCouponDeductsZero() {
        assertThat(adapter.couponDeduction(null, 5L, new BigDecimal("20.00"), false))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void activityPriceAndCouponAreMutuallyExclusive() {
        assertThatThrownBy(() -> adapter.couponDeduction(1L, 5L, new BigDecimal("20.00"), true))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不可叠加");
    }

    @Test
    void fullReductionCouponNeedsThresholdOnPreCouponAmount() {
        when(couponUserMapper.selectById(1L)).thenReturn(userCoupon(1, 11, 5L, 1, null));
        MktCoupon template = new MktCoupon();
        template.setId(11L);
        template.setCouponType(1);
        template.setThreshold(new BigDecimal("28.00"));
        template.setAmount(new BigDecimal("6.00"));
        when(couponMapper.selectById(11L)).thenReturn(template);

        // 券前 20 元未满 28 → 拒绝；券前 30 元只减 6 元，运费另算
        assertThatThrownBy(() -> adapter.couponDeduction(1L, 5L, new BigDecimal("20.00"), false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("满减券");
        assertThat(adapter.couponDeduction(1L, 5L, new BigDecimal("30.00"), false))
                .isEqualByComparingTo("6.00");
    }

    @Test
    void discountCouponUsesRateAndCapsAtItemAmount() {
        when(couponUserMapper.selectById(1L)).thenReturn(userCoupon(1, 12, 5L, 1, null));
        MktCoupon template = new MktCoupon();
        template.setId(12L);
        template.setCouponType(2);
        template.setDiscount(new BigDecimal("0.80"));
        when(couponMapper.selectById(12L)).thenReturn(template);

        assertThat(adapter.couponDeduction(1L, 5L, new BigDecimal("30.00"), false))
                .isEqualByComparingTo("6.00");
    }

    @Test
    void deductionCannotExceedItemAmount() {
        when(couponUserMapper.selectById(1L)).thenReturn(userCoupon(1, 13, 5L, 1, null));
        MktCoupon template = new MktCoupon();
        template.setId(13L);
        template.setCouponType(3);
        template.setAmount(new BigDecimal("99.00"));
        when(couponMapper.selectById(13L)).thenReturn(template);

        assertThat(adapter.couponDeduction(1L, 5L, new BigDecimal("20.00"), false))
                .isEqualByComparingTo("20.00");
    }

    @Test
    void couponOfAnotherUserOrAlreadyUsedIsRejected() {
        when(couponUserMapper.selectById(1L)).thenReturn(userCoupon(1, 11, 6L, 1, null));
        assertThatThrownBy(() -> adapter.couponDeduction(1L, 5L, new BigDecimal("20.00"), false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不属于当前用户");

        when(couponUserMapper.selectById(2L)).thenReturn(userCoupon(2, 11, 5L, 2, 77L));
        assertThatThrownBy(() -> adapter.couponDeduction(2L, 5L, new BigDecimal("20.00"), false))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已使用");
    }

    @Test
    void lockCouponFailsWhenAnotherOrderGrabbedItFirst() {
        when(couponUserMapper.selectById(1L)).thenReturn(userCoupon(1, 11, 5L, 1, null));
        when(couponUserMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> adapter.lockCoupon(1L, 5L, 900L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("刚被占用");
    }

    @SuppressWarnings("unchecked")
    private com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MktCouponUser> captureUpdate() {
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<MktCouponUser>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(couponUserMapper).update(isNull(), captor.capture());
        return captor.getValue();
    }

    @Test
    void lockCouponBindsTheOrderOnly() {
        when(couponUserMapper.selectById(1L)).thenReturn(userCoupon(1, 11, 5L, 1, null));
        when(couponUserMapper.update(isNull(), any())).thenReturn(1);

        adapter.lockCoupon(1L, 5L, 900L);

        var wrapper = captureUpdate();
        // 只写 order_id，不改 status：锁定态仍是「未使用」，支付后才核销
        assertThat(wrapper.getSqlSet()).contains("order_id");
        assertThat(wrapper.getSqlSet()).doesNotContain("status");
        // 条件 UPDATE 同时限定 id + 未使用 + 未绑定订单，并发抢券时只会有一行成功
        assertThat(wrapper.getSqlSegment()).contains("id").contains("status").contains("order_id");
    }

    @Test
    void releaseCouponReturnsItToUnused() {
        adapter.releaseCoupon(900L);

        var wrapper = captureUpdate();
        assertThat(wrapper.getSqlSet()).contains("status").contains("order_id").contains("use_time");
        assertThat(wrapper.getSqlSegment()).contains("order_id").contains("IN");
    }

    @Test
    void useCouponMarksUsedOnlyForLockedRows() {
        adapter.useCoupon(900L);

        var wrapper = captureUpdate();
        assertThat(wrapper.getSqlSet()).contains("status").contains("use_time");
        assertThat(wrapper.getSqlSegment()).contains("order_id").contains("status");
    }

    /** 04 篇 §3.3：秒杀抢的是活动独立配额，扣减走条件 UPDATE，抢不到就失败，不会超卖。 */
    @Test
    void seckillReservesIndependentQuotaOnly() {
        MktActivity activity = seckill(2L, "{\"seckillPrice\":1.00}");
        activity.setTotalStock(10);
        when(activityMapper.selectById(2L)).thenReturn(activity);
        when(activityMapper.deductSeckillStock(2L, 4)).thenReturn(1);

        adapter.reserveSeckillStock(2L, 5L, 4);
        verify(activityMapper).deductSeckillStock(2L, 4);
    }

    /** 04 篇 §3.3：配额被抢完时条件 UPDATE 影响 0 行，必须在建单前就失败。 */
    @Test
    void seckillFailsWhenQuotaIsGone() {
        MktActivity activity = seckill(2L, "{\"seckillPrice\":1.00}");
        activity.setTotalStock(10);
        when(activityMapper.selectById(2L)).thenReturn(activity);
        when(activityMapper.deductSeckillStock(2L, 11)).thenReturn(0);

        assertThatThrownBy(() -> adapter.reserveSeckillStock(2L, 5L, 11))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已抢完");
    }

    /** 04 篇 §3.3：每人限购按历史有效订单件数累计，超过就拒。 */
    @Test
    void seckillPerUserLimitBlocksOverBuy() {
        MktActivity activity = seckill(2L, "{\"seckillPrice\":1.00,\"perUserLimit\":5}");
        activity.setTotalStock(100);
        when(activityMapper.selectById(2L)).thenReturn(activity);
        when(orderItemMapper.sumQtyByUserAndActivity(2L, 5L)).thenReturn(4);

        assertThatThrownBy(() -> adapter.reserveSeckillStock(2L, 5L, 2))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("每人限购 5 件");
        verify(activityMapper, never()).deductSeckillStock(any(), anyInt());
    }

    /** 未配配额（total_stock 为 NULL）= 不限量，只查限购；拼团活动完全不碰秒杀库存。 */
    @Test
    void unquotaedSeckillAndGroupBuySkipStock() {
        MktActivity noQuota = seckill(2L, "{\"seckillPrice\":1.00}");
        when(activityMapper.selectById(2L)).thenReturn(noQuota);
        adapter.reserveSeckillStock(2L, 5L, 3);
        verify(activityMapper, never()).deductSeckillStock(any(), anyInt());

        when(activityMapper.selectById(1L)).thenReturn(activity(1L, 1, null, null, "{\"groupPrice\":1.50}", 1));
        adapter.reserveSeckillStock(1L, 5L, 3);
        adapter.releaseSeckillStock(1L, 3);
        verify(activityMapper, never()).deductSeckillStock(any(), anyInt());
        verify(activityMapper, never()).restoreSeckillStock(any(), anyInt());
    }

    /** 取消/退款归还配额（04 篇 §3.3），活动结束也要还，否则配额被死单占住。 */
    @Test
    void releaseSeckillStockRestoresQuota() {
        MktActivity activity = seckill(2L, "{\"seckillPrice\":1.00}");
        activity.setTotalStock(10);
        when(activityMapper.selectById(2L)).thenReturn(activity);

        adapter.releaseSeckillStock(2L, 4);
        verify(activityMapper).restoreSeckillStock(2L, 4);
    }

    /** 内存闸门拦住时不该碰数据库：削峰的意义就是把抢不到的请求挡在库之前。 */
    @Test
    void gateRejectionSkipsDatabase() {
        MktActivity activity = seckill(2L, "{\"seckillPrice\":1.00}");
        activity.setTotalStock(10);
        when(activityMapper.selectById(2L)).thenReturn(activity);
        StockGate gate = mock(StockGate.class);
        when(gate.tryDeduct(any(), eq(10), eq(4))).thenReturn(false);
        MarketingAdapterImpl gated = new MarketingAdapterImpl(activityMapper, couponMapper, couponUserMapper,
                skuMapper, orderItemMapper, new ObjectMapper(), gate);

        assertThatThrownBy(() -> gated.reserveSeckillStock(2L, 5L, 4))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已抢完");
        verify(activityMapper, never()).deductSeckillStock(any(), anyInt());
    }

    /** 闸门放行但条件 UPDATE 未命中（预扣状态失真）：必须归还内存预扣，否则配额凭空少一份。 */
    @Test
    void databaseRejectionRestoresGateQuota() {
        MktActivity activity = seckill(2L, "{\"seckillPrice\":1.00}");
        activity.setTotalStock(10);
        when(activityMapper.selectById(2L)).thenReturn(activity);
        when(activityMapper.deductSeckillStock(2L, 4)).thenReturn(0);
        StockGate gate = mock(StockGate.class);
        when(gate.tryDeduct(any(), eq(10), eq(4))).thenReturn(true);
        MarketingAdapterImpl gated = new MarketingAdapterImpl(activityMapper, couponMapper, couponUserMapper,
                skuMapper, orderItemMapper, new ObjectMapper(), gate);

        assertThatThrownBy(() -> gated.reserveSeckillStock(2L, 5L, 4))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已抢完");
        verify(gate).restore("seckill:2", 4);
    }
}
