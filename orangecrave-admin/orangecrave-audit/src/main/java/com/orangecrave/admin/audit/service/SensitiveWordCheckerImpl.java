package com.orangecrave.admin.audit.service;

import com.orangecrave.admin.audit.entity.AudSensitiveWord;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.text.SensitiveWordChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 把 audit 的敏感词词典暴露给写文本的那些模块（见 05 篇 §2.3）。 */
@Service
@RequiredArgsConstructor
public class SensitiveWordCheckerImpl implements SensitiveWordChecker {

    private final SensitiveWordService sensitiveWordService;

    @Override
    public String check(String text) {
        AudSensitiveWord hit = sensitiveWordService.hit(text);
        if (hit == null) {
            return text;
        }
        // 提示文案带上违规词，顾客才知道要改什么（01 篇 aud_sensitive_word）
        if ("拒绝".equals(hit.getAction())) {
            throw new BizException("内容包含违规词「" + hit.getWord() + "」，请修改后重新提交");
        }
        return sensitiveWordService.mask(text);
    }
}
