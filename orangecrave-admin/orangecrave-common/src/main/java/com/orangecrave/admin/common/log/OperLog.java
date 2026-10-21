package com.orangecrave.admin.common.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要记入 sys_oper_log 的操作（见 01 篇 §1「必须记快照的操作」白名单）。
 * 敏感操作在服务内通过 {@link OperLogContext} 提供改前/改后快照。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 模块名，如「系统配置」。*/
    String module();

    /** 操作类型，如 UPDATE / FORCE_REFUND / AUDIT */
    String type();

    /** 描述 */
    String desc();
}
