package com.orangecrave.admin.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.order.adapter.WaybillAdapter;
import com.orangecrave.admin.order.entity.OmsDelivery;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.mapper.OmsDeliveryMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 发货（见 03 篇 §5）。面单走 WaybillAdapter（v1 桩）。  */
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final OmsDeliveryMapper deliveryMapper;
    private final OrderService orderService;
    private final WaybillAdapter waybillAdapter;

    @Transactional(rollbackFor = Exception.class)
    public OmsDelivery ship(Long orderId, String company, String deliveryNo, Long operatorId) {
        OmsOrder order = orderService.get(orderId);
        String no = deliveryNo;
        if (no == null || no.isBlank()) {
            no = waybillAdapter.createWaybill(order);
        }
        if (!orderService.deliver(orderId, operatorId)) {
            throw new BizException("订单不可发货");
        }
        OmsDelivery delivery = new OmsDelivery();
        delivery.setOrderId(orderId);
        delivery.setDeliveryCompany(company);
        delivery.setDeliveryNo(no);
        delivery.setDeliveryTime(LocalDateTime.now());
        delivery.setStatus(1);
        deliveryMapper.insert(delivery);
        return delivery;
    }

    /** 批量发货（见 02 篇 §2.6 delivery/batch）。 */
    @Transactional(rollbackFor = Exception.class)
    public List<OmsDelivery> batchShip(List<ShipItem> items, Long operatorId) {
        List<OmsDelivery> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (ShipItem item : items) {
            result.add(ship(item.getOrderId(), item.getCompany(), item.getDeliveryNo(), operatorId));
        }
        return result;
    }

    /** 电子面单打印数据（见 02 篇 §2.6 delivery/print）。未录入运单的先按桩生成。 */
    public List<Map<String, Object>> printWaybills(List<Long> orderIds) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (orderIds == null) {
            return rows;
        }
        for (Long orderId : orderIds) {
            OmsOrder order = orderService.get(orderId);
            OmsDelivery delivery = byOrder(orderId);
            String no = delivery != null ? delivery.getDeliveryNo() : waybillAdapter.createWaybill(order);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", orderId);
            row.put("orderNo", order.getOrderNo());
            row.put("company", delivery == null ? null : delivery.getDeliveryCompany());
            row.put("deliveryNo", no);
            row.put("receiverSnapshot", order.getReceiverSnapshot());
            rows.add(row);
        }
        return rows;
    }

    /** 物流轨迹（见 02 篇 §2.6 delivery/track）。 */
    public List<Map<String, Object>> track(String deliveryNo) {
        return waybillAdapter.queryTrace(deliveryNo);
    }

    public OmsDelivery byOrder(Long orderId) {
        return deliveryMapper.selectOne(new LambdaQueryWrapper<OmsDelivery>()
                .eq(OmsDelivery::getOrderId, orderId).orderByDesc(OmsDelivery::getId).last("limit 1"));
    }

    @Data
    public static class ShipItem {
        private Long orderId;
        private String company;
        private String deliveryNo;
    }
}
