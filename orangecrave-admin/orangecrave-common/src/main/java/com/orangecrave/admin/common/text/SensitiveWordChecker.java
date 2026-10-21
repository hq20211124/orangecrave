package com.orangecrave.admin.common.text;

/**
 * 顾客可见文本的敏感词审核（05 篇 §2.3：作品名、合集名、订单备注、工单内容）。
 *
 * <p>接口放在 common 是因为写这些文本的模块分散在 order、work、service，
 * 而审核词典属于 audit；由各模块反向依赖 audit 改成这里的一个端口，避免模块成环。 */
public interface SensitiveWordChecker {

    /**
     * 命中「拒绝」类词直接抛业务异常（提示文案带违规词，见 01 篇 §aud_sensitive_word）；
     * 命中「替换」类词返回打码后的文本；未命中原样返回。
     */
    String check(String text);
}
