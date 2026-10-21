package com.orangecrave.admin.order.adapter;

import com.orangecrave.admin.finance.entity.RefundLedger;
import com.orangecrave.admin.order.entity.OmsOrder;

import java.math.BigDecimal;

/**
 * 退款适配器（见 03 篇 §10）。v1 桩：直接写 fin_refund_ledger 成功流水。  */
public interface RefundAdapter {

    /** 发起退款，返回退款单号。 */
    String refund(OmsOrder order, BigDecimal amount, int bizType, String remark);

    /**
     * 执行一张已复核通过的退款申请（03 篇 §3.3 两段式的第二段）。
     * 流水已在申请时落库，这里只与渠道通信，不再新增流水。 */
    void refundByLedger(RefundLedger ledger);
}
