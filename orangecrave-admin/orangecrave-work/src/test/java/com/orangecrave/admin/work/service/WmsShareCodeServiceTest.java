package com.orangecrave.admin.work.service;

import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.finance.service.FinRefundService;
import com.orangecrave.admin.goods.entity.PmsSpu;
import com.orangecrave.admin.goods.mapper.PmsSpuMapper;
import com.orangecrave.admin.work.entity.WmsShareCode;
import com.orangecrave.admin.work.entity.WmsWork;
import com.orangecrave.admin.work.mapper.WmsShareCodeMapper;
import com.orangecrave.admin.work.mapper.WmsWorkMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 无料分享发起校验、核销与退款（01 篇 §4 / 03 篇 §7.3 / 04 篇 §2.3）。
 * 发起时校验起订量；核销走条件 UPDATE，预付分摊按套数算，到期只退未核销部分。 */
class WmsShareCodeServiceTest {

    private WmsShareCodeMapper shareCodeMapper;
    private FinRefundService finRefundService;
    private WmsWorkMapper workMapper;
    private PmsSpuMapper spuMapper;
    private WmsShareCodeService service;

    @BeforeEach
    void setUp() {
        shareCodeMapper = mock(WmsShareCodeMapper.class);
        finRefundService = mock(FinRefundService.class);
        workMapper = mock(WmsWorkMapper.class);
        spuMapper = mock(PmsSpuMapper.class);
        service = new WmsShareCodeService(shareCodeMapper, finRefundService, workMapper, spuMapper);
    }

    /** 分享者预付 100 元买 10 套，领 3 套就核销 30 元（按套数分摊）。 */
    @Test
    void redeemSplitsPrepayByUnits() {
        when(shareCodeMapper.selectOne(any())).thenReturn(prepayCode());
        when(shareCodeMapper.redeem(eq(7L), eq(3), any())).thenReturn(1);

        WmsShareCode code = service.redeem("FREE3", 3);

        assertThat(code.getPayerType()).isEqualTo(2);
        ArgumentCaptor<BigDecimal> amount = ArgumentCaptor.forClass(BigDecimal.class);
        verify(shareCodeMapper).redeem(eq(7L), eq(3), amount.capture());
        assertThat(amount.getValue()).isEqualByComparingTo("30.00");
    }

    /** 上限判断写在条件 UPDATE 的 WHERE 里，抢不到行就是被别人用完了。 */
    @Test
    void redeemFailsWhenQuotaAlreadyTaken() {
        when(shareCodeMapper.selectOne(any())).thenReturn(prepayCode());
        when(shareCodeMapper.redeem(anyLong(), anyInt(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.redeem("FREE3", 3))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("刚被用完");
    }

    /** 超过可下单套数上限直接拒，不等到 UPDATE。 */
    @Test
    void redeemRejectsOverLimit() {
        WmsShareCode code = prepayCode();
        code.setUsedQty(9);
        when(shareCodeMapper.selectOne(any())).thenReturn(code);

        assertThatThrownBy(() -> service.redeem("FREE3", 2))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(WmsShareCodeService.REASON_OVER_LIMIT);
        verify(shareCodeMapper, never()).redeem(anyLong(), anyInt(), any());
    }

    /** 失效与过期的码不能被领取（01 篇 §4 的三种失效原因）。 */
    @Test
    void redeemRejectsInvalidOrExpiredCode() {
        WmsShareCode invalid = prepayCode();
        invalid.setStatus(0);
        invalid.setInvalidReason("关联作品已封禁");
        when(shareCodeMapper.selectOne(any())).thenReturn(invalid);
        assertThatThrownBy(() -> service.redeem("FREE3", 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("关联作品已封禁");

        WmsShareCode expired = prepayCode();
        expired.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(shareCodeMapper.selectOne(any())).thenReturn(expired);
        assertThatThrownBy(() -> service.redeem("FREE3", 1))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(WmsShareCodeService.REASON_EXPIRED);
    }

    /** 到期：状态条件 UPDATE 抢到才退未核销余额，重复触发不会退两次。 */
    @Test
    void expireRefundsUnusedPrepayOnce() {
        WmsShareCode code = prepayCode();
        code.setPrepayUsed(new BigDecimal("30.00"));
        when(shareCodeMapper.markExpired(7L, WmsShareCodeService.REASON_EXPIRED)).thenReturn(1);
        when(shareCodeMapper.selectById(7L)).thenReturn(code);

        service.expire(7L);

        ArgumentCaptor<BigDecimal> refunded = ArgumentCaptor.forClass(BigDecimal.class);
        verify(finRefundService).record(eq(3), eq(7L), isNull(), eq(9L), refunded.capture(),
                eq("分享码到期，退未核销预付款"));
        assertThat(refunded.getValue()).isEqualByComparingTo("70.00");
        ArgumentCaptor<WmsShareCode> update = ArgumentCaptor.forClass(WmsShareCode.class);
        verify(shareCodeMapper).updateById(update.capture());
        assertThat(update.getValue().getPrepayRefunded()).isEqualByComparingTo("70.00");

        // 第二次触发时条件 UPDATE 影响 0 行，不再退款
        when(shareCodeMapper.markExpired(7L, WmsShareCodeService.REASON_EXPIRED)).thenReturn(0);
        service.expire(7L);
        verify(finRefundService, times(1)).record(anyInt(), anyLong(), isNull(), anyLong(), any(), any());
    }

    /** 全额核销完的码到期没有余额可退。 */
    @Test
    void expireWithoutBalanceRefundsNothing() {
        WmsShareCode code = prepayCode();
        code.setPrepayUsed(new BigDecimal("100.00"));
        when(shareCodeMapper.markExpired(7L, WmsShareCodeService.REASON_EXPIRED)).thenReturn(1);
        when(shareCodeMapper.selectById(7L)).thenReturn(code);

        service.expire(7L);

        verify(finRefundService, never()).record(anyInt(), anyLong(), any(), anyLong(), any(), any());
    }

    /** 04 篇 §2.3：可领套数达不到起订量时不能发起无料分享，提示还差几件。 */
    @Test
    void createRejectsBelowMinOrderQty() {
        stubWorkAndSpu(10);

        assertThatThrownBy(() -> service.create(9L, 3L, 2, 5, new BigDecimal("100.00"), null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("起订量为 10 件，还差 5 件");
        verify(shareCodeMapper, never()).insert(any());
    }

    /** 达到起订量即放行：无料分享 payerType=2，预付与初始状态落库。 */
    @Test
    void createPassesWhenQuotaMeetsMinOrderQty() {
        stubWorkAndSpu(10);
        when(shareCodeMapper.selectCount(any())).thenReturn(0L);

        WmsShareCode code = service.create(9L, 3L, 2, 10, new BigDecimal("100.00"),
                LocalDateTime.now().plusDays(7));

        assertThat(code.getShareCode()).startsWith("SC");
        assertThat(code.getPayerType()).isEqualTo(2);
        assertThat(code.getStatus()).isEqualTo(1);
        assertThat(code.getMaxOrderQty()).isEqualTo(10);
        ArgumentCaptor<WmsShareCode> saved = ArgumentCaptor.forClass(WmsShareCode.class);
        verify(shareCodeMapper).insert(saved.capture());
        assertThat(saved.getValue().getPrepayAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getValue().getPrepayUsed()).isEqualByComparingTo("0.00");
        assertThat(saved.getValue().getWorkId()).isEqualTo(3L);
    }

    /** 作品不存在直接拒，不生成码。 */
    @Test
    void createRejectsUnknownWork() {
        when(workMapper.selectById(3L)).thenReturn(null);

        assertThatThrownBy(() -> service.create(9L, 3L, 2, 10, null, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("作品不存在");
        verify(shareCodeMapper, never()).insert(any());
    }

    /** 作品 3 号关联 SPU 2 号，起订量由入参给定。 */
    private void stubWorkAndSpu(int minOrderQty) {
        WmsWork work = new WmsWork();
        work.setId(3L);
        work.setSpuId(2L);
        when(workMapper.selectById(3L)).thenReturn(work);
        PmsSpu spu = new PmsSpu();
        spu.setId(2L);
        spu.setSpuName("测试作品");
        spu.setMinOrderQty(minOrderQty);
        when(spuMapper.selectById(2L)).thenReturn(spu);
    }

    private WmsShareCode prepayCode() {
        WmsShareCode code = new WmsShareCode();
        code.setId(7L);
        code.setShareCode("FREE3");
        code.setUserId(9L);
        code.setShareType(2);
        code.setPayerType(2);
        code.setMaxOrderQty(10);
        code.setUsedQty(0);
        code.setStatus(1);
        code.setPrepayAmount(new BigDecimal("100.00"));
        code.setPrepayUsed(BigDecimal.ZERO);
        code.setPrepayRefunded(BigDecimal.ZERO);
        return code;
    }
}
