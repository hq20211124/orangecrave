package com.orangecrave.admin.work.constant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作品状态（见 05 篇 §3，来自线上 workStatus 映射）。1 正常为【推断】。  */
public final class WorkStatus {

    private WorkStatus() {
    }

    public static final int NORMAL = 1;
    public static final int PENDING_AUDIT = 2;
    public static final int TO_MODIFY = 3;
    public static final int OFF_SHELF = 5;
    public static final int EXPIRED = 6;
    public static final int VIOLATION = 7;

    private static final Map<Integer, String> NAMES = new ConcurrentHashMap<>();

    static {
        NAMES.put(NORMAL, "正常");
        NAMES.put(PENDING_AUDIT, "待审核");
        NAMES.put(TO_MODIFY, "待修改");
        NAMES.put(OFF_SHELF, "商品下架");
        NAMES.put(EXPIRED, "作品过期");
        NAMES.put(VIOLATION, "作品违规");
    }

    public static String name(Integer status) {
        return status == null ? null : NAMES.getOrDefault(status, "未知");
    }
}
