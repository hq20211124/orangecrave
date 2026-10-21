package com.orangecrave.admin.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.marketing.entity.MktInvite;
import com.orangecrave.admin.marketing.mapper.MktInviteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 邀请好友（见 04 篇 §3.5）。拉新归因：被邀请人首次下单时记录邀请关系；
 * v1 后台只做记录查询与拉新统计，奖励券发放走 mkt_coupon。 */
@Service
@RequiredArgsConstructor
public class MktInviteService {

    public static final int RECORDED = 1;
    public static final int EFFECTIVE = 2;
    public static final int INVALID = 3;

    private final MktInviteMapper inviteMapper;

    public PageResult<MktInvite> page(PageQuery query, Long activityId) {
        Page<MktInvite> page = inviteMapper.selectPage(query.toPage(), new LambdaQueryWrapper<MktInvite>()
                .eq(activityId != null, MktInvite::getActivityId, activityId)
                .orderByDesc(MktInvite::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /** 写一条邀请关系（顾客端首次下单归因时调用，见 04 篇 §3.5）。 */
    public MktInvite record(Long activityId, Long inviterUserId, Long inviteeUserId, Long orderId) {
        MktInvite invite = new MktInvite();
        invite.setActivityId(activityId);
        invite.setInviterUserId(inviterUserId);
        invite.setInviteeUserId(inviteeUserId);
        invite.setOrderId(orderId);
        invite.setStatus(orderId == null ? RECORDED : EFFECTIVE);
        invite.setInviterRewardGranted(0);
        invite.setInviteeRewardGranted(0);
        inviteMapper.insert(invite);
        return invite;
    }

    /** 拉新统计（见 02 篇 §2.8 invite/statistics）。 */
    public Map<String, Object> statistics(Long activityId) {
        List<MktInvite> all = inviteMapper.selectList(new LambdaQueryWrapper<MktInvite>()
                .eq(activityId != null, MktInvite::getActivityId, activityId));
        long effective = all.stream().filter(i -> i.getStatus() != null && i.getStatus() == EFFECTIVE).count();

        Map<Long, long[]> byActivity = new LinkedHashMap<>();
        for (MktInvite invite : all) {
            long[] stat = byActivity.computeIfAbsent(invite.getActivityId(), k -> new long[2]);
            stat[0]++;
            if (invite.getStatus() != null && invite.getStatus() == EFFECTIVE) {
                stat[1]++;
            }
        }
        List<Map<String, Object>> activities = new ArrayList<>();
        for (Map.Entry<Long, long[]> entry : byActivity.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("activityId", entry.getKey());
            row.put("inviteCount", entry.getValue()[0]);
            row.put("effectiveCount", entry.getValue()[1]);
            activities.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalInvites", all.size());
        result.put("effectiveInvites", effective);
        result.put("conversionRate", all.isEmpty() ? 0.0 : (double) effective / all.size());
        result.put("byActivity", activities);
        return result;
    }
}
