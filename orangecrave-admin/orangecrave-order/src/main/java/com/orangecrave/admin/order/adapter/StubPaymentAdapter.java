package com.orangecrave.admin.order.adapter;

import com.orangecrave.admin.order.entity.OmsOrder;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付桩：v1 只打日志，视为同步成功。  */
@Slf4j
public class StubPaymentAdapter implements PaymentAdapter {

    @Override
    public void pay(OmsOrder order) {
        log.info("[支付桩] 订单 {} 视为支付成功，生成模拟交易号", order.getOrderNo());
    }
}
