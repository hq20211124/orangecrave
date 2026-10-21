package com.orangecrave.admin.work.task;

import com.orangecrave.admin.work.mapper.WmsWorkMapper;
import com.orangecrave.admin.work.service.WmsWorkService;
import com.orangecrave.admin.task.handler.DelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 作品过期（00 篇 §5）：状态转 workStatus 6「作品过期」，不改其他状态的作品。 */
@Component
@RequiredArgsConstructor
public class WorkExpireHandler implements DelayTaskHandler {

    public static final String TASK_WORK_EXPIRE = "WORK_EXPIRE";

    private final WmsWorkMapper workMapper;
    private final WmsWorkService workService;

    @Override
    public String taskType() {
        return TASK_WORK_EXPIRE;
    }

    @Override
    public void execute(Long bizId) {
        workService.expire(bizId);
    }

    @Override
    public List<Long> scanOverdue() {
        return workMapper.selectExpiredIds();
    }
}
