package com.orangecrave.admin.order.adapter;

import com.orangecrave.admin.finance.entity.RefundLedger;
import com.orangecrave.admin.finance.service.FinRefundService;
import com.orangecrave.admin.order.entity.OmsOrder;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * 退款桩：写 fin_refund_ledger 一条成功流水，不实际调用微信。  */
@RequiredArgsConstructor
public class StubRefundAdapter implements RefundAdapter {

    private final FinRefundService finRefundService;

    @Override
    public String refund(OmsOrder order, BigDecimal amount, int bizType, String remark) {
        RefundLedger ledger = finRefundService.record(bizType, order.getId(), order.getId(),
                order.getPayerUserId() == null ? order.getUserId() : order.getPayerUserId(), amount, remark);
        return ledger.getRefundNo();
    }

    /** 桩：真实实现这里调微信退款 API；流水已由申请环节落库，只补一句日志。 */
    @Override
    public void refundByLedger(RefundLedger ledger) {
        // 桩不产生渠道单号，直接把流水置为成功
        finRefundService.markExecuted(ledger.getId());
    }
}
