package com.orangecrave.admin.order.adapter;

import com.orangecrave.admin.order.entity.OmsOrder;

/**
 * 生产适配器（见 03 篇 §4.2）。v1 桩：仓管手工录工单号、手工推进状态。  */
public interface ProductionAdapter {

    /** 创建生产工单，返回 MES 工单号（v1 手工，返回 null）。**/
    String createWorkOrder(OmsOrder order);

    /** 从 MES 同步生产状态（v1 手工触发，桩仅记录；将来调 MES API 并回写状态）。 */
    void syncStatus(OmsOrder order);
}
