package com.orangecrave.admin.order.adapter;

import com.orangecrave.admin.order.entity.OmsOrder;

import java.util.List;
import java.util.Map;

/**
 * 电子面单适配器（见 03 篇 §5.1）。v1 桩：返回规则化假单号。  */
public interface WaybillAdapter {

    String createWaybill(OmsOrder order);

    /** 物流轨迹查询（见 02 篇 §2.6 delivery/track）。v1 桩返回模拟节点。 */
    List<Map<String, Object>> queryTrace(String deliveryNo);
}
