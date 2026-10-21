package com.orangecrave.admin.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.goods.entity.PmsSku;
import com.orangecrave.admin.goods.entity.PmsSpu;
import com.orangecrave.admin.goods.mapper.PmsSkuMapper;
import com.orangecrave.admin.goods.mapper.PmsSpuMapper;
import com.orangecrave.admin.goods.model.PricingRequest;
import com.orangecrave.admin.goods.model.PricingResult;
import com.orangecrave.admin.goods.service.FreightService;
import com.orangecrave.admin.goods.service.PricingService;
import com.orangecrave.admin.audit.service.AuditService;
import com.orangecrave.admin.common.text.SensitiveWordChecker;
import com.orangecrave.admin.finance.entity.RefundLedger;
import com.orangecrave.admin.finance.service.FinRefundService;
import com.orangecrave.admin.order.adapter.MarketingAdapter;
import com.orangecrave.admin.order.adapter.PaymentAdapter;
import com.orangecrave.admin.order.adapter.ProductionAdapter;
import com.orangecrave.admin.order.adapter.RefundAdapter;
import com.orangecrave.admin.order.constant.OrderStatus;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.entity.OmsOrderItem;
import com.orangecrave.admin.order.mapper.OmsOrderImageMapper;
import com.orangecrave.admin.order.mapper.OmsOrderItemMapper;
import com.orangecrave.admin.order.mapper.OmsOrderMapper;
import com.orangecrave.admin.order.mapper.OmsOrderStatusLogMapper;
import com.orangecrave.admin.order.model.OrderItemInput;
import com.orangecrave.admin.order.model.OrderPreviewDTO;
import com.orangecrave.admin.order.model.OrderPreviewVO;
import com.orangecrave.admin.order.model.OrderSubmitDTO;
import com.orangecrave.admin.system.service.SysConfigService;
import com.orangecrave.admin.task.engine.DelayTaskEngine;
import com.orangecrave.admin.work.service.WmsShareCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单状态机并发语义测试（03 篇 §2.4）。核心：条件 UPDATE 影响行数为 0 时不得写状态日志/副作用。 */
class OrderServiceTest {

    private OmsOrderMapper orderMapper;
    private OmsOrderItemMapper itemMapper;
    private OmsOrderStatusLogMapper statusLogMapper;
    private PmsSpuMapper spuMapper;
    private PmsSkuMapper skuMapper;
    private PricingService pricingService;
    private FreightService freightService;
    private com.orangecrave.admin.order.adapter.MarketingAdapter marketingAdapter;
    private com.orangecrave.admin.finance.service.FinRefundService finRefundService;
    private com.orangecrave.admin.order.adapter.RefundAdapter refundAdapter;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderMapper = Mockito.mock(OmsOrderMapper.class);
        itemMapper = Mockito.mock(OmsOrderItemMapper.class);
        statusLogMapper = Mockito.mock(OmsOrderStatusLogMapper.class);
        spuMapper = Mockito.mock(PmsSpuMapper.class);
        skuMapper = Mockito.mock(PmsSkuMapper.class);
        pricingService = Mockito.mock(PricingService.class);
        freightService = Mockito.mock(FreightService.class);
        marketingAdapter = Mockito.mock(com.orangecrave.admin.order.adapter.MarketingAdapter.class);
        finRefundService = Mockito.mock(com.orangecrave.admin.finance.service.FinRefundService.class);
        refundAdapter = Mockito.mock(com.orangecrave.admin.order.adapter.RefundAdapter.class);
        orderService = new OrderService(
                orderMapper,
                itemMapper,
                Mockito.mock(OmsOrderImageMapper.class),
                statusLogMapper,
                spuMapper,
                skuMapper,
                pricingService,
                freightService,
                Mockito.mock(SysConfigService.class),
                Mockito.mock(DelayTaskEngine.class),
                Mockito.mock(PaymentAdapter.class),
                Mockito.mock(ProductionAdapter.class),
                refundAdapter,
                finRefundService,
                Mockito.mock(com.orangecrave.admin.work.service.WmsShareCodeService.class),
                marketingAdapter,
                Mockito.mock(SensitiveWordChecker.class),
                Mockito.mock(AuditService.class),
                new ObjectMapper());
    }

    @Test
    void cancelWinsRaceWhenTransitionAffectsOneRow() {
        OmsOrder order = new OmsOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.UNPAID);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.transition(1L, OrderStatus.UNPAID, OrderStatus.CANCELLED)).thenReturn(1);

        assertThat(orderService.cancel(1L, "系统", null)).isTrue();
        verify(statusLogMapper, times(1)).insert(any());
    }

    @Test
    void cancelNoOpWhenAlreadyPaid() {
        OmsOrder order = new OmsOrder();
        order.setId(1L);
        order.setStatus(OrderStatus.UNPAID);
        when(orderMapper.selectById(1L)).thenReturn(order);
        // 另一竞争者已改状态，条件 UPDATE 影响 0 行
        when(orderMapper.transition(1L, OrderStatus.UNPAID, OrderStatus.CANCELLED)).thenReturn(0);

        assertThat(orderService.cancel(1L, "系统", null)).isFalse();
        verify(statusLogMapper, never()).insert(any());
    }

    @Test
    void paidOrderCannotBeCancelled() {
        OmsOrder order = new OmsOrder();
        order.setId(2L);
        order.setStatus(OrderStatus.PENDING_AUDIT);
        when(orderMapper.selectById(2L)).thenReturn(order);

        assertThat(orderService.cancel(2L, "系统", null)).isFalse();
        verify(orderMapper, never()).transition(anyLong(), anyInt(), anyInt());
    }

    @Test
    void payRejectsNonUnpaidOrder() {
        OmsOrder order = new OmsOrder();
        order.setId(3L);
        order.setStatus(OrderStatus.PENDING_AUDIT);
        when(orderMapper.selectById(3L)).thenReturn(order);

        assertThatThrownBy(() -> orderService.pay(3L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不可支付");
    }

    @Test
    void payTransitionsUnpaidToPendingAudit() {
        OmsOrder order = new OmsOrder();
        order.setId(4L);
        order.setStatus(OrderStatus.UNPAID);
        order.setUserId(9L);
        when(orderMapper.selectById(4L)).thenReturn(order);
        when(orderMapper.transition(4L, OrderStatus.UNPAID, OrderStatus.PENDING_AUDIT)).thenReturn(1);

        orderService.pay(4L);
        verify(orderMapper, times(1)).updateById(any());
    }

    @Test
    void illegalRejectUsesStatus13() {
        OmsOrder order = new OmsOrder();
        order.setId(5L);
        order.setStatus(OrderStatus.PENDING_AUDIT);
        when(orderMapper.selectById(5L)).thenReturn(order);
        when(orderMapper.transition(5L, OrderStatus.PENDING_AUDIT, OrderStatus.ILLEGAL_CANCELLED)).thenReturn(1);

        assertThat(orderService.rejectIllegal(5L, "团长身份认证未通过")).isTrue();
    }

    @Test
    void guardBlocksIllegalSourceStateBeforeAuditPass() {
        OmsOrder order = new OmsOrder();
        order.setId(6L);
        order.setStatus(OrderStatus.UNPAID);
        when(orderMapper.selectById(6L)).thenReturn(order);

        // 待付款 → 待生产 不在 03 篇 §2.3 流转表内，守卫必须在不发 UPDATE 的前提下拦下
        assertThat(orderService.auditPass(6L, 1L)).isFalse();
        verify(orderMapper, never()).transition(anyLong(), anyInt(), anyInt());
        verify(statusLogMapper, never()).insert(any());
    }

    @Test
    void manualUpdateRejectsTargetOutsideTransitionTable() {
        OmsOrder order = new OmsOrder();
        order.setId(7L);
        order.setStatus(OrderStatus.PRODUCING);
        when(orderMapper.selectById(7L)).thenReturn(order);

        assertThatThrownBy(() -> orderService.manualUpdateStatus(7L, OrderStatus.UNPAID, 1L, "回退"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("非法状态流转");
    }

    @Test
    void aftersalePathIsBoundedByTransitionTable() {
        OmsOrder order = new OmsOrder();
        order.setId(8L);
        order.setStatus(OrderStatus.DELIVERED);
        when(orderMapper.selectById(8L)).thenReturn(order);
        when(orderMapper.transition(8L, OrderStatus.DELIVERED, OrderStatus.AFTERSALE)).thenReturn(1);

        assertThat(orderService.transitionTo(8L, OrderStatus.AFTERSALE, "用户", 1L, "申请售后")).isTrue();
        assertThat(OrderStatus.canTransition(OrderStatus.AFTERSALE, OrderStatus.REFUNDED)).isTrue();
        // 已发货不能一步跳到已退款（03 篇 §2.3 必须经售后中）
        assertThat(OrderStatus.canTransition(OrderStatus.DELIVERED, OrderStatus.REFUNDED)).isFalse();
    }

    @Test
    void forceRefundApplicationMovesNoMoney() {
        OmsOrder order = new OmsOrder();
        order.setId(9L);
        order.setStatus(OrderStatus.PENDING_PRODUCTION);
        order.setUserId(7L);
        order.setPayAmount(new java.math.BigDecimal("88.00"));
        when(orderMapper.selectById(9L)).thenReturn(order);
        RefundLedger ledger = new RefundLedger();
        ledger.setId(900L);
        when(finRefundService.recordPendingReview(anyInt(), anyLong(), anyLong(), anyLong(), any(), any()))
                .thenReturn(ledger);

        // 03 篇 §3.3 两段式第一段：只落一条待复核流水，钱与订单状态都不动
        assertThat(orderService.applyForceRefund(9L, null, "客诉补偿", 1L)).isEqualTo(900L);
        verify(orderMapper, never()).transition(anyLong(), anyInt(), anyInt());
        verify(refundAdapter, never()).refundByLedger(any());
    }

    @Test
    void forceRefundRejectsOverpayAndDuplicateApplication() {
        OmsOrder order = new OmsOrder();
        order.setId(9L);
        order.setStatus(OrderStatus.PENDING_PRODUCTION);
        order.setPayAmount(new java.math.BigDecimal("88.00"));
        when(orderMapper.selectById(9L)).thenReturn(order);

        assertThatThrownBy(() -> orderService.applyForceRefund(9L, new java.math.BigDecimal("89.00"), "补偿", 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能超过实付金额");

        when(finRefundService.pendingForceReview(9L)).thenReturn(new RefundLedger());
        assertThatThrownBy(() -> orderService.applyForceRefund(9L, null, "补偿", 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已有一笔强制退款申请");
    }

    @Test
    void forceRefundReviewBypassesTableButKeepsConditionalUpdate() {
        OmsOrder order = new OmsOrder();
        order.setId(9L);
        order.setStatus(OrderStatus.PENDING_PRODUCTION);
        order.setPayAmount(new java.math.BigDecimal("88.00"));
        when(orderMapper.selectById(9L)).thenReturn(order);
        when(finRefundService.get(900L)).thenReturn(pendingForceLedger());
        when(finRefundService.markReviewed(900L, true, 3L, "同意")).thenReturn(true);
        when(orderMapper.transition(9L, OrderStatus.PENDING_PRODUCTION, OrderStatus.REFUNDED)).thenReturn(1);

        // 03 篇 §3.3：强制退款按设计绕过正常流程，但仍走条件 UPDATE
        assertThat(orderService.reviewForceRefund(900L, true, "同意", 3L)).isEqualTo("RF900");
        verify(refundAdapter).refundByLedger(any());
        verify(orderMapper, times(1)).transition(9L, OrderStatus.PENDING_PRODUCTION, OrderStatus.REFUNDED);
        verify(statusLogMapper, times(1)).insert(any());
    }

    @Test
    void secondForceRefundReviewerLosesTheRace() {
        OmsOrder order = new OmsOrder();
        order.setId(9L);
        order.setStatus(OrderStatus.PENDING_PRODUCTION);
        when(orderMapper.selectById(9L)).thenReturn(order);
        when(finRefundService.get(900L)).thenReturn(pendingForceLedger());
        // 另一名财务已抢先复核，条件 UPDATE 影响 0 行
        when(finRefundService.markReviewed(900L, true, 4L, "同意")).thenReturn(false);

        assertThatThrownBy(() -> orderService.reviewForceRefund(900L, true, "同意", 4L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("其他财务");
        verify(refundAdapter, never()).refundByLedger(any());
        verify(orderMapper, never()).transition(anyLong(), anyInt(), anyInt());
    }

    @Test
    void rejectedForceRefundLeavesOrderStatusAlone() {
        OmsOrder order = new OmsOrder();
        order.setId(9L);
        order.setStatus(OrderStatus.PENDING_PRODUCTION);
        when(orderMapper.selectById(9L)).thenReturn(order);
        when(finRefundService.get(900L)).thenReturn(pendingForceLedger());
        when(finRefundService.markReviewed(900L, false, 3L, "金额不符")).thenReturn(true);

        assertThat(orderService.reviewForceRefund(900L, false, "金额不符", 3L)).isNull();
        verify(refundAdapter, never()).refundByLedger(any());
        verify(orderMapper, never()).transition(anyLong(), anyInt(), anyInt());
    }

    private RefundLedger pendingForceLedger() {
        RefundLedger ledger = new RefundLedger();
        ledger.setId(900L);
        ledger.setRefundNo("RF900");
        ledger.setOrderId(9L);
        ledger.setUserId(7L);
        ledger.setAmount(new java.math.BigDecimal("88.00"));
        ledger.setReviewStatus(1);
        ledger.setRemark("客诉补偿");
        return ledger;
    }

    @Test
    void cancelRestoresSeckillQuotaOnlyWhenTransitionWins() {
        OmsOrderItem item = new OmsOrderItem();
        item.setQty(3);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));

        OmsOrder order = new OmsOrder();
        order.setId(14L);
        order.setStatus(OrderStatus.UNPAID);
        order.setActivityId(66L);
        when(orderMapper.selectById(14L)).thenReturn(order);
        when(orderMapper.transition(14L, OrderStatus.UNPAID, OrderStatus.CANCELLED)).thenReturn(1);

        assertThat(orderService.cancel(14L, "用户", 7L)).isTrue();
        verify(marketingAdapter).releaseSeckillStock(66L, 3);

        OmsOrder loser = new OmsOrder();
        loser.setId(15L);
        loser.setStatus(OrderStatus.UNPAID);
        loser.setActivityId(66L);
        when(orderMapper.selectById(15L)).thenReturn(loser);
        // 条件 UPDATE 影响 0 行：单没取消成，配额也不能还
        when(orderMapper.transition(15L, OrderStatus.UNPAID, OrderStatus.CANCELLED)).thenReturn(0);

        assertThat(orderService.cancel(15L, "用户", 7L)).isFalse();
        verify(marketingAdapter, times(1)).releaseSeckillStock(66L, 3);
    }

    /**
     * 03 篇 §4.3：MES 车间术语先落到后台状态，返回给用户的是粗粒度文案。
     */
    @Test
    void mesStatusMapsToCoarseUserProgress() {
        OmsOrder order = new OmsOrder();
        order.setId(16L);
        order.setStatus(OrderStatus.PENDING_PRODUCTION);
        when(orderMapper.selectById(16L)).thenReturn(order);
        when(orderMapper.transition(16L, OrderStatus.PENDING_PRODUCTION, OrderStatus.PRODUCING)).thenReturn(1);

        assertThat(orderService.syncProduction(16L, "已开料")).isEqualTo("正在制作");
        verify(statusLogMapper, times(1)).insert(any());

        OmsOrder unknown = new OmsOrder();
        unknown.setId(17L);
        unknown.setStatus(OrderStatus.PRODUCING);
        when(orderMapper.selectById(17L)).thenReturn(unknown);
        assertThatThrownBy(() -> orderService.syncProduction(17L, "已装箱"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("未知的 MES 状态");
    }

    /** 04 篇 §4 顺序：活动价覆盖原价 → 券抵扣 → 券前金额判包邮 → 运费。 */
    private void stubGoodsAndPricing(String itemAmount) {
        PmsSku sku = new PmsSku();
        sku.setId(1L);
        sku.setSpuId(2L);
        PmsSpu spu = new PmsSpu();
        spu.setId(2L);
        spu.setFreightTemplateId(1L);
        when(skuMapper.selectById(1L)).thenReturn(sku);
        when(spuMapper.selectById(2L)).thenReturn(spu);
        stubBatchQueries();

        PricingResult priced = new PricingResult();
        priced.setBasePrice(new java.math.BigDecimal(itemAmount));
        priced.setCraftPrice(java.math.BigDecimal.ZERO);
        priced.setItemAmount(new java.math.BigDecimal(itemAmount));
        when(pricingService.compute(any(PricingRequest.class), any(PmsSku.class), any(PmsSpu.class)))
                .thenReturn(priced);
    }

    /**
     * 让 selectBatchIds 在被调用时按 id 逐个转发到 selectById。
     * 这样测试里后来补的商品（第二个 SKU）也能被批量查询查到，不用重复登记。 */
    private void stubBatchQueries() {
        when(skuMapper.selectBatchIds(any())).thenAnswer(invocation ->
                resolve(invocation.getArgument(0), id -> skuMapper.selectById(id)));
        when(spuMapper.selectBatchIds(any())).thenAnswer(invocation ->
                resolve(invocation.getArgument(0), id -> spuMapper.selectById(id)));
    }

    /**
     * 商品查询数必须与行数解耦：1 行和 3 行都只应该查 2 次（一次 SKU 批量 + 一次 SPU 批量）。
     * 这条是防退化的——谁再把查询写回循环里，这里立刻红。 */
    @Test
    void goodsQueryCountDoesNotGrowWithLineCount() {
        stubGoodsAndPricing("20.00");
        registerGoods(3L, 4L, "手机壳");
        registerGoods(5L, 6L, "卡册");
        when(marketingAdapter.couponDeduction(isNull(), eq(7L), any(), eq(false)))
                .thenReturn(java.math.BigDecimal.ZERO);
        when(freightService.compute(eq(1L), any(), anyInt())).thenReturn(java.math.BigDecimal.ZERO);

        int oneLine = countGoodsQueries(() ->
                orderService.preview(previewOf(List.of(item(1L, 10, null)))));
        int threeLines = countGoodsQueries(() -> orderService.preview(
                previewOf(List.of(item(1L, 10, null), item(3L, 5, null), item(5L, 2, null)))));

        System.out.printf("[sql] 商品查询次数: 1 行=%d, 3 行=%d%n", oneLine, threeLines);
        assertThat(oneLine).isEqualTo(2);
        assertThat(threeLines).isEqualTo(oneLine);
    }

    private OrderPreviewDTO previewOf(List<OrderItemInput> items) {
        OrderPreviewDTO dto = new OrderPreviewDTO();
        dto.setUserId(7L);
        dto.setItems(items);
        return dto;
    }

    private void registerGoods(Long skuId, Long spuId, String name) {
        PmsSku sku = new PmsSku();
        sku.setId(skuId);
        sku.setSpuId(spuId);
        when(skuMapper.selectById(skuId)).thenReturn(sku);
        PmsSpu spu = new PmsSpu();
        spu.setId(spuId);
        spu.setSpuName(name);
        spu.setFreightTemplateId(1L);
        when(spuMapper.selectById(spuId)).thenReturn(spu);
    }

    /**
     * 跑一次动作，统计商品相关的查询次数。
     * 只数 selectBatchIds——测试桩里 selectBatchIds 是按 id 转发到 selectById 实现的，
     * 那些转发调用是桩的内部行为，不是真实 SQL。 */
    private int countGoodsQueries(Runnable action) {
        int before = countNamed(skuMapper, "selectBatchIds") + countNamed(spuMapper, "selectBatchIds");
        action.run();
        int after = countNamed(skuMapper, "selectBatchIds") + countNamed(spuMapper, "selectBatchIds");
        return after - before;
    }

    private int countNamed(Object mock, String method) {
        int count = 0;
        for (Invocation invocation : Mockito.mockingDetails(mock).getInvocations()) {
            if (invocation.getMethod().getName().equals(method)) {
                count++;
            }
        }
        return count;
    }

    private <T> List<T> resolve(Object rawIds, Function<Long, T> loader) {
        List<T> out = new ArrayList<>();
        for (Object id : (Collection<?>) rawIds) {
            T value = loader.apply((Long) id);
            if (value != null) {
                out.add(value);
            }
        }
        return out;
    }

    @Test
    void activityUnitPriceIsPassedIntoPricing() {
        stubGoodsAndPricing("30.00");
        when(marketingAdapter.activityUnitPrice(1L, 1L, 2L)).thenReturn(new java.math.BigDecimal("1.50"));
        when(marketingAdapter.couponDeduction(isNull(), eq(7L), any(), eq(true))).thenReturn(java.math.BigDecimal.ZERO);
        when(freightService.compute(eq(1L), any(), eq(20))).thenReturn(new java.math.BigDecimal("4.00"));

        OrderPreviewDTO dto = new OrderPreviewDTO();
        dto.setSkuId(1L);
        dto.setQty(20);
        dto.setUserId(7L);
        dto.setActivityId(1L);
        PricingResult result = orderService.preview(dto);

        ArgumentCaptor<PricingRequest> captor = ArgumentCaptor.forClass(PricingRequest.class);
        verify(pricingService).compute(captor.capture(), any(PmsSku.class), any(PmsSpu.class));
        assertThat(captor.getValue().getActivityUnitPrice()).isEqualByComparingTo("1.50");
        assertThat(result.getPayAmount()).isEqualByComparingTo("34.00");
    }

    @Test
    void freeShippingThresholdUsesPreCouponAmount() {
        stubGoodsAndPricing("30.00");
        // 券减 6 元后只剩 24 元，但包邮判定仍按券前 30 元（04 篇 §4 步骤 3）
        when(marketingAdapter.couponDeduction(eq(5L), eq(7L), any(), eq(false))).thenReturn(new java.math.BigDecimal("6.00"));
        when(freightService.compute(eq(1L), any(), eq(20))).thenReturn(java.math.BigDecimal.ZERO);

        OrderPreviewDTO dto = new OrderPreviewDTO();
        dto.setSkuId(1L);
        dto.setQty(20);
        dto.setUserId(7L);
        dto.setCouponUserId(5L);
        PricingResult result = orderService.preview(dto);

        verify(freightService).compute(1L, new java.math.BigDecimal("30.00"), 20);
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("6.00");
        assertThat(result.getPayAmount()).isEqualByComparingTo("24.00");
    }

    @Test
    void couponIsReleasedOnlyWhenCancelWins() {
        OmsOrder order = new OmsOrder();
        order.setId(10L);
        order.setStatus(OrderStatus.UNPAID);
        when(orderMapper.selectById(10L)).thenReturn(order);
        when(orderMapper.transition(10L, OrderStatus.UNPAID, OrderStatus.CANCELLED)).thenReturn(1);

        assertThat(orderService.cancel(10L, "用户", 7L)).isTrue();
        verify(marketingAdapter).releaseCoupon(10L);

        OmsOrder other = new OmsOrder();
        other.setId(11L);
        other.setStatus(OrderStatus.UNPAID);
        when(orderMapper.selectById(11L)).thenReturn(other);
        when(orderMapper.transition(11L, OrderStatus.UNPAID, OrderStatus.CANCELLED)).thenReturn(0);

        assertThat(orderService.cancel(11L, "用户", 7L)).isFalse();
        verify(marketingAdapter, never()).releaseCoupon(11L);
    }

    @Test
    void payConsumesTheCouponAfterSuccessfulTransition() {
        OmsOrder order = new OmsOrder();
        order.setId(12L);
        order.setStatus(OrderStatus.UNPAID);
        order.setUserId(7L);
        order.setCouponId(5L);
        when(orderMapper.selectById(12L)).thenReturn(order);
        when(orderMapper.transition(12L, OrderStatus.UNPAID, OrderStatus.PENDING_AUDIT)).thenReturn(1);

        orderService.pay(12L);
        verify(marketingAdapter).useCoupon(12L);
    }

    // ---- 03 篇 §1.3 一单多件混单 ----

    /** 两个 SKU 各一行：金额相加、运费按整单只收一次、行级字段不直通。 */
    @Test
    void mixedOrderSumsLinesAndChargesFreightOnce() {
        stubGoodsAndPricing("20.00");
        stubSecondGoods();
        when(pricingService.compute(any(PricingRequest.class), any(PmsSku.class), any(PmsSpu.class)))
                .thenAnswer(inv -> {
            PricingRequest req = inv.getArgument(0);
            PricingResult r = new PricingResult();
            r.setBasePrice(new java.math.BigDecimal("2.00"));
            r.setItemAmount(new java.math.BigDecimal("2.00").multiply(
                    java.math.BigDecimal.valueOf(req.getQty())));
            return r;
        });
        when(marketingAdapter.couponDeduction(isNull(), eq(7L), any(), eq(false)))
                .thenReturn(java.math.BigDecimal.ZERO);
        when(freightService.compute(eq(1L), any(), eq(15))).thenReturn(new java.math.BigDecimal("4.00"));

        OrderPreviewDTO dto = new OrderPreviewDTO();
        dto.setUserId(7L);
        dto.setItems(List.of(item(1L, 10, null), item(3L, 5, null)));

        PricingResult result = orderService.preview(dto);

        assertThat(result.getItemAmount()).isEqualByComparingTo("30.00");
        assertThat(result.getFreight()).isEqualByComparingTo("4.00");
        assertThat(result.getPayAmount()).isEqualByComparingTo("34.00");
        // 整单没有行级字段
        assertThat(result.getBasePrice()).isNull();
        // 运费模板取第一行，基数与件数是整单
        verify(freightService).compute(1L, new java.math.BigDecimal("30.00"), 15);
    }

    /** 混单预览另给逐行明细：每行小计 + 商品快照，整单聚合字段不变（老读法兼容）。 */
    @Test
    void previewExposesPerLineSubtotals() {
        stubGoodsAndPricing("20.00");
        spuMapper.selectById(2L).setSpuName("测试作品");
        stubSecondGoods();
        when(pricingService.compute(any(PricingRequest.class), any(PmsSku.class), any(PmsSpu.class)))
                .thenAnswer(inv -> {
            PricingRequest req = inv.getArgument(0);
            PricingResult r = new PricingResult();
            r.setBasePrice(new java.math.BigDecimal("2.00"));
            r.setItemAmount(new java.math.BigDecimal("2.00").multiply(
                    java.math.BigDecimal.valueOf(req.getQty())));
            return r;
        });
        when(marketingAdapter.couponDeduction(isNull(), eq(7L), any(), eq(false)))
                .thenReturn(java.math.BigDecimal.ZERO);
        when(freightService.compute(eq(1L), any(), eq(15))).thenReturn(new java.math.BigDecimal("4.00"));

        OrderPreviewDTO dto = new OrderPreviewDTO();
        dto.setUserId(7L);
        dto.setItems(List.of(item(1L, 10, null), item(3L, 5, null)));

        OrderPreviewVO vo = orderService.preview(dto);

        // 聚合口径与老契约一致
        assertThat(vo.getItemAmount()).isEqualByComparingTo("30.00");
        assertThat(vo.getPayAmount()).isEqualByComparingTo("34.00");
        // 逐行：数量、小计、商品名
        assertThat(vo.getLines()).hasSize(2);
        assertThat(vo.getLines()).extracting(OrderPreviewVO.Line::getQty)
                .containsExactly(10, 5);
        assertThat(vo.getLines()).extracting(OrderPreviewVO.Line::getItemAmount)
                .containsExactly(new java.math.BigDecimal("20.00"), new java.math.BigDecimal("10.00"));
        assertThat(vo.getLines()).extracting(OrderPreviewVO.Line::getGoodsName)
                .containsExactly("测试作品", "手机壳");
    }

    @Test
    void lineBelowMinOrderQtyIsRejectedWithShortfall() {
        stubGoodsAndPricing("30.00");
        stubMinAndIncrement(10, 5);

        OrderPreviewDTO dto = new OrderPreviewDTO();
        dto.setUserId(7L);
        dto.setSkuId(1L);
        dto.setQty(6);

        assertThatThrownBy(() -> orderService.preview(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("起订量为 10 件，还差 4 件");
    }

    @Test
    void lineQtyMustBeMultipleOfIncrement() {
        stubGoodsAndPricing("30.00");
        stubMinAndIncrement(10, 5);

        OrderPreviewDTO dto = new OrderPreviewDTO();
        dto.setUserId(7L);
        dto.setSkuId(1L);
        dto.setQty(12);

        assertThatThrownBy(() -> orderService.preview(dto))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("数量须为 5 的整数倍");
    }

    @Test
    void mixedSubmitReservesSeckillPerLineAndWritesOneRowPerLine() {
        stubGoodsAndPricing("20.00");
        stubSecondGoods();
        when(pricingService.compute(any(PricingRequest.class), any(PmsSku.class), any(PmsSpu.class)))
                .thenAnswer(inv -> {
            PricingRequest req = inv.getArgument(0);
            PricingResult r = new PricingResult();
            r.setBasePrice(new java.math.BigDecimal("2.00"));
            r.setItemAmount(new java.math.BigDecimal("2.00").multiply(
                    java.math.BigDecimal.valueOf(req.getQty())));
            return r;
        });
        when(marketingAdapter.couponDeduction(isNull(), eq(7L), any(), eq(false)))
                .thenReturn(java.math.BigDecimal.ZERO);
        when(freightService.compute(eq(1L), any(), eq(15))).thenReturn(new java.math.BigDecimal("4.00"));

        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setUserId(7L);
        dto.setItems(List.of(item(1L, 10, 1L), item(3L, 5, 2L)));

        orderService.submit(dto);

        // 秒杀按行抢各自活动的配额
        verify(marketingAdapter).reserveSeckillStock(1L, 7L, 10);
        verify(marketingAdapter).reserveSeckillStock(2L, 7L, 5);
        // 每行一条明细，行级活动各自落库
        ArgumentCaptor<OmsOrderItem> itemCaptor = ArgumentCaptor.forClass(OmsOrderItem.class);
        verify(itemMapper, times(2)).insert(itemCaptor.capture());
        assertThat(itemCaptor.getAllValues()).allSatisfy(i -> assertThat(i.getOrderNo()).startsWith("SO"));
        assertThat(itemCaptor.getAllValues().get(0).getActivityId()).isEqualTo(1L);
        assertThat(itemCaptor.getAllValues().get(0).getQty()).isEqualTo(10);
        assertThat(itemCaptor.getAllValues().get(1).getActivityId()).isEqualTo(2L);
        assertThat(itemCaptor.getAllValues().get(1).getQty()).isEqualTo(5);
    }

    @Test
    void cancelReturnsSeckillQuotaPerLineActivity() {
        OmsOrder order = new OmsOrder();
        order.setId(20L);
        order.setStatus(OrderStatus.UNPAID);
        order.setActivityId(1L);
        when(orderMapper.selectById(20L)).thenReturn(order);
        when(orderMapper.transition(20L, OrderStatus.UNPAID, OrderStatus.CANCELLED)).thenReturn(1);

        OmsOrderItem line1 = new OmsOrderItem();
        line1.setActivityId(1L);
        line1.setQty(10);
        OmsOrderItem line2 = new OmsOrderItem();
        line2.setActivityId(2L);
        line2.setQty(5);
        OmsOrderItem line3 = new OmsOrderItem();
        line3.setActivityId(null);
        line3.setQty(3);
        when(itemMapper.selectList(any())).thenReturn(List.of(line1, line2, line3));

        assertThat(orderService.cancel(20L, "用户", 7L)).isTrue();

        // 按活动聚合归还：活动 1 还 10、活动 2 还 5，无活动的行不还
        verify(marketingAdapter).releaseSeckillStock(1L, 10);
        verify(marketingAdapter).releaseSeckillStock(2L, 5);
    }

    /** 单行简写字段仍然可用（小程序确认页老契约），且走同一条混单链路。 */
    @Test
    void legacySingleLineShorthandStillWorks() {
        stubGoodsAndPricing("20.00");
        when(marketingAdapter.couponDeduction(isNull(), eq(7L), any(), eq(false)))
                .thenReturn(java.math.BigDecimal.ZERO);
        when(freightService.compute(eq(1L), any(), eq(10))).thenReturn(new java.math.BigDecimal("4.00"));

        OrderSubmitDTO dto = new OrderSubmitDTO();
        dto.setUserId(7L);
        dto.setSkuId(1L);
        dto.setQty(10);

        orderService.submit(dto);

        verify(marketingAdapter).reserveSeckillStock(isNull(), eq(7L), eq(10));
        ArgumentCaptor<OmsOrderItem> itemCaptor = ArgumentCaptor.forClass(OmsOrderItem.class);
        verify(itemMapper, times(1)).insert(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getQty()).isEqualTo(10);
    }

    private OrderItemInput item(Long skuId, int qty, Long activityId) {
        OrderItemInput line = new OrderItemInput();
        line.setSkuId(skuId);
        line.setQty(qty);
        line.setActivityId(activityId);
        return line;
    }

    /** 第二个 SKU（id=3 / spu=4），复用同一运费模板。 */
    private void stubSecondGoods() {
        PmsSku sku = new PmsSku();
        sku.setId(3L);
        sku.setSpuId(4L);
        PmsSpu spu = new PmsSpu();
        spu.setId(4L);
        spu.setSpuName("手机壳");
        spu.setFreightTemplateId(1L);
        when(skuMapper.selectById(3L)).thenReturn(sku);
        when(spuMapper.selectById(4L)).thenReturn(spu);
    }

    private void stubMinAndIncrement(int min, int increment) {
        PmsSpu spu = spuMapper.selectById(2L);
        spu.setMinOrderQty(min);
        spu.setIncrementQty(increment);
    }
}
