package com.orangecrave.admin.common.log;

import java.util.HashMap;
import java.util.Map;

/**
 * 敏感操作快照上下文（见 01 篇 §1 sys_oper_log）。服务在事务内调用 setBefore/setAfter。
 * 由 OperLog 切面写入 sys_oper_log.before_data / after_data。 */
public final class OperLogContext {

    private static final ThreadLocal<Map<String, String>> HOLDER = ThreadLocal.withInitial(HashMap::new);

    private OperLogContext() {
    }

    public static void setBefore(String json) {
        HOLDER.get().put("before", json);
    }

    public static void setAfter(String json) {
        HOLDER.get().put("after", json);
    }

    public static String getBefore() {
        return HOLDER.get().get("before");
    }

    public static String getAfter() {
        return HOLDER.get().get("after");
    }

    public static void clear() {
        HOLDER.remove();
    }
}
