package com.orangecrave.admin.common.util;

/**
 * 出参脱敏（02 篇 §2.3：身份证号脱敏返回）。 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /** 保留前 3 位与后 4 位，中间全部打码；过短的直接全打码，宁可少给也不能漏。 */
    public static String idCard(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= 7) {
            return "*".repeat(value.length());
        }
        return value.substring(0, 3) + "*".repeat(value.length() - 7) + value.substring(value.length() - 4);
    }
}
