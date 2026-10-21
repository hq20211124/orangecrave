package com.orangecrave.admin.order.constant;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单状态（见 03 篇 §2.1）。含非法订单专用状态 13。 */
public final class OrderStatus {

    private OrderStatus() {
    }

    public static final int UNPAID = 0;
    public static final int PENDING_GROUP = 1;
    public static final int PENDING_AUDIT = 2;
    public static final int PENDING_PRODUCTION = 3;
    public static final int PRODUCING = 4;
    public static final int QC = 5;
    public static final int PENDING_DELIVERY = 6;
    public static final int DELIVERED = 7;
    public static final int COMPLETED = 8;
    public static final int CANCELLED = 9;
    public static final int AFTERSALE = 10;
    public static final int REFUNDED = 11;
    /** 非法订单 → 已取消（团长身份认证未通过等被拦截） */
    public static final int ILLEGAL_CANCELLED = 13;

    private static final Map<Integer, String> NAMES = new ConcurrentHashMap<>();

    /** 合法流转表（03 篇 §2.3）。守卫条件由这张表生成，不再散落在各 service 里。 */
    private static final Map<Integer, Set<Integer>> TRANSITIONS = Map.of(
            UNPAID, Set.of(PENDING_AUDIT, PENDING_GROUP, CANCELLED, ILLEGAL_CANCELLED),
            PENDING_GROUP, Set.of(PENDING_AUDIT, CANCELLED, ILLEGAL_CANCELLED),
            PENDING_AUDIT, Set.of(PENDING_PRODUCTION, CANCELLED, ILLEGAL_CANCELLED),
            PENDING_PRODUCTION, Set.of(PRODUCING),
            PRODUCING, Set.of(QC),
            QC, Set.of(PENDING_DELIVERY),
            PENDING_DELIVERY, Set.of(DELIVERED),
            DELIVERED, Set.of(COMPLETED, AFTERSALE),
            AFTERSALE, Set.of(REFUNDED)
    );

    static {
        NAMES.put(UNPAID, "待付款");
        NAMES.put(PENDING_GROUP, "待成团");
        NAMES.put(PENDING_AUDIT, "待审核");
        NAMES.put(PENDING_PRODUCTION, "待生产");
        NAMES.put(PRODUCING, "生产中");
        NAMES.put(QC, "质检中");
        NAMES.put(PENDING_DELIVERY, "待发货");
        NAMES.put(DELIVERED, "已发货");
        NAMES.put(COMPLETED, "已完成");
        NAMES.put(CANCELLED, "已取消");
        NAMES.put(AFTERSALE, "售后中");
        NAMES.put(REFUNDED, "已退款");
        NAMES.put(ILLEGAL_CANCELLED, "已取消");
    }

    public static String name(Integer status) {
        return status == null ? null : NAMES.getOrDefault(status, "未知");
    }

    /** from → to 是否在流转表里。终态（已完成 / 已取消 / 已退款 / 状态 13）没有出边。 */
    public static boolean canTransition(Integer from, int to) {
        return from != null && TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
