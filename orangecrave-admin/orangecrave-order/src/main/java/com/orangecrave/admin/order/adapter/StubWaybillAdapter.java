package com.orangecrave.admin.order.adapter;

import com.orangecrave.admin.order.entity.OmsOrder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 面单桩：返回 SF + 时间戳。  */
public class StubWaybillAdapter implements WaybillAdapter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter SHOW_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String createWaybill(OmsOrder order) {
        return "SF" + LocalDateTime.now().format(FMT) + order.getId();
    }

    @Override
    public List<Map<String, Object>> queryTrace(String deliveryNo) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(node("已揽收", "快件已由快递员揽收"));
        // v1 桩：仅返回揽收节点，真实实现由快递 100 / 菜鸟返回全链路
        return nodes;
    }

    private Map<String, Object> node(String status, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("time", LocalDateTime.now().format(SHOW_FMT));
        m.put("status", status);
        m.put("desc", desc);
        return m;
    }
}
