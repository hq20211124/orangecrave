package com.orangecrave.admin.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orangecrave.admin.finance.entity.RefundLedger;
import com.orangecrave.admin.finance.mapper.RefundLedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 退款流水（见 01 篇 §9）。所有退款都经此落流水，不冲减销售额。  */
@Service
@RequiredArgsConstructor
public class FinRefundService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQ = new AtomicInteger();

    private final RefundLedgerMapper refundLedgerMapper;

    public RefundLedger record(int bizType, Long bizId, Long orderId, Long userId,
                               BigDecimal amount, String remark) {
        // v1 退款桩：自动流程直接记为成功（见 03 篇 §10）
        return insert(bizType, bizId, orderId, userId, amount, remark, 2, 0);
    }

    /**
     * 强制退款申请（03 篇 §3.3 两段式）：先落一条待复核流水，钱在这一动里不动，
     * 只有 {@link #markReviewed} 通过后才由订单侧执行退款。 */
    public RefundLedger recordPendingReview(int bizType, Long bizId, Long orderId, Long userId,
                                            BigDecimal amount, String remark) {
        return insert(bizType, bizId, orderId, userId, amount, remark, 1, 1);
    }

    /** 复核结论：通过 → 仍「处理中」等退款执行；驳回 → 直接失败。条件 UPDATE 保证两名财务同时点只有一次生效。 */
    public boolean markReviewed(Long ledgerId, boolean approved, Long reviewerId, String reviewRemark) {
        return refundLedgerMapper.markReviewed(ledgerId, approved ? 2 : 3, approved ? 1 : 3,
                reviewerId, reviewRemark) == 1;
    }

    /** 退款已执行（桩调用返回后），流水转「成功」。 */
    public void markExecuted(Long ledgerId) {
        refundLedgerMapper.markExecuted(ledgerId);
    }

    public RefundLedger get(Long ledgerId) {
        return refundLedgerMapper.selectById(ledgerId);
    }

    /** 该订单是否已有待财务复核的强制退款申请（03 篇 §3.3），防重复提交。 */
    public RefundLedger pendingForceReview(Long orderId) {
        return refundLedgerMapper.selectOne(new LambdaQueryWrapper<RefundLedger>()
                .eq(RefundLedger::getOrderId, orderId)
                .eq(RefundLedger::getReviewStatus, 1)
                .orderByDesc(RefundLedger::getId)
                .last("LIMIT 1"));
    }

    private RefundLedger insert(int bizType, Long bizId, Long orderId, Long userId,
                                BigDecimal amount, String remark, int status, int reviewStatus) {
        RefundLedger ledger = new RefundLedger();
        ledger.setRefundNo("RF" + LocalDateTime.now().format(NO_FMT)
                + String.format("%04d", SEQ.incrementAndGet() % 10000));
        ledger.setBizType(bizType);
        ledger.setBizId(bizId);
        ledger.setOrderId(orderId);
        ledger.setUserId(userId);
        ledger.setAmount(amount);
        ledger.setChannel(1);
        ledger.setStatus(status);
        ledger.setReviewStatus(reviewStatus);
        ledger.setRemark(remark);
        refundLedgerMapper.insert(ledger);
        return ledger;
    }
}
