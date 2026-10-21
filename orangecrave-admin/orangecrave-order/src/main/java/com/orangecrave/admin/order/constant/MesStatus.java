package com.orangecrave.admin.order.constant;

import com.orangecrave.admin.common.exception.BizException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MES 状态映射（见 03 篇 §4.3）。
 * <p>MES 的术语按车间工序走（已开料 / 裱纸中…），直接怼给用户看是灾难，
 * 所以车间状态先落到后台订单状态，再由这里给出对用户展示的粗粒度文案。
 * 将来接真实 MES 时只改这张表，业务代码不动（03 篇 §4.2）。 */
public final class MesStatus {

    private MesStatus() {
    }

    /** 车间术语 → {后台订单状态, 对用户展示文案} */
    private static final Map<String, Mapping> TABLE = new LinkedHashMap<>();

    static {
        TABLE.put("已开料", new Mapping(OrderStatus.PRODUCING, "正在制作"));
        TABLE.put("已上机", new Mapping(OrderStatus.PRODUCING, "正在制作"));
        TABLE.put("待后道", new Mapping(OrderStatus.PRODUCING, "正在制作"));
        TABLE.put("裱纸中", new Mapping(OrderStatus.PRODUCING, "正在制作"));
        TABLE.put("品检", new Mapping(OrderStatus.QC, "质检中"));
        TABLE.put("完工", new Mapping(OrderStatus.PENDING_DELIVERY, "等待发货"));
    }

    public static Mapping require(String mesStatus) {
        Mapping mapping = mesStatus == null ? null : TABLE.get(mesStatus.trim());
        if (mapping == null) {
            throw new BizException("未知的 MES 状态：" + mesStatus);
        }
        return mapping;
    }

    public static Map<String, Mapping> table() {
        return Collections.unmodifiableMap(TABLE);
    }

    /** 后台状态 + 用户文案。 */
    public record Mapping(int orderStatus, String userText) {
    }
}
