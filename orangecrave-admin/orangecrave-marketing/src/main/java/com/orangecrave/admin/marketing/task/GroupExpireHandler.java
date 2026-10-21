package com.orangecrave.admin.marketing.task;

import com.orangecrave.admin.marketing.mapper.MktGroupMapper;
import com.orangecrave.admin.marketing.service.MktGroupService;
import com.orangecrave.admin.task.handler.DelayTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 拼团到期（00 篇 §5「拼团未成团 24 小时 → 拼团失败 + 自动退款」、03 篇 §7.1）。
 * 与「最后一个成员付款」竞争：谁抢到 mkt_group 的条件 UPDATE 谁收口。 */
@Component
@RequiredArgsConstructor
public class GroupExpireHandler implements DelayTaskHandler {

    public static final String TASK_GROUP_EXPIRE = "GROUP_EXPIRE";

    private final MktGroupMapper groupMapper;
    private final MktGroupService groupService;

    @Override
    public String taskType() {
        return TASK_GROUP_EXPIRE;
    }

    @Override
    public void execute(Long bizId) {
        groupService.expire(bizId);
    }

    @Override
    public List<Long> scanOverdue() {
        return groupMapper.selectExpiredIds();
    }
}
