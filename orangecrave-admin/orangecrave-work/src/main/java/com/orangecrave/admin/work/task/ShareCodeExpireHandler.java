package com.orangecrave.admin.work.task;

import com.orangecrave.admin.work.mapper.WmsShareCodeMapper;
import com.orangecrave.admin.work.service.WmsShareCodeService;
import com.orangecrave.admin.task.handler.DelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分享码到期（00 篇 §5 / 03 篇 §7.3）：失效 + 退未核销预付款。
 * 幂等靠 wms_share_code 上的条件 UPDATE，退不到款时不会再退一遍。 */
@Component
@RequiredArgsConstructor
public class ShareCodeExpireHandler implements DelayTaskHandler {

    public static final String TASK_SHARE_CODE_EXPIRE = "SHARE_CODE_EXPIRE";

    private final WmsShareCodeMapper shareCodeMapper;
    private final WmsShareCodeService shareCodeService;

    @Override
    public String taskType() {
        return TASK_SHARE_CODE_EXPIRE;
    }

    @Override
    public void execute(Long bizId) {
        shareCodeService.expire(bizId);
    }

    @Override
    public List<Long> scanOverdue() {
        return shareCodeMapper.selectExpiredIds();
    }
}
