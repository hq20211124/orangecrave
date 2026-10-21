package com.orangecrave.admin.order.adapter;

import com.orangecrave.admin.order.entity.OmsOrder;
import lombok.extern.slf4j.Slf4j;

/**
 * 生产桩：不生成 MES 工单，返回 null，由仓管手工录入。  */
@Slf4j
public class ManualProductionAdapter implements ProductionAdapter {

    @Override
    public String createWorkOrder(OmsOrder order) {
        log.info("[生产桩] 订单 {} 转待生产，等待仓管手工录入 MES 工单号", order.getOrderNo());
        return null;
    }

    @Override
    public void syncStatus(OmsOrder order) {
        log.info("[生产桩] 订单 {} 手工触发 MES 同步，状态保持不变（v1）", order.getOrderNo());
    }
}
