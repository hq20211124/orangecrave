package com.orangecrave.admin.order.adapter;

import com.orangecrave.admin.order.entity.OmsOrder;

/**
 * 支付适配器（见 03 篇 §10）。v1 桩：同步成功，不真正调起微信支付。  */
public interface PaymentAdapter {

    /** 发起支付；桩实现直接视为成功。*/
    void pay(OmsOrder order);
}
