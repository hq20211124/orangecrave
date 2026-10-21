package com.orangecrave.admin.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.marketing.entity.MktGroup;
import com.orangecrave.admin.marketing.entity.MktGroupMember;
import com.orangecrave.admin.marketing.mapper.MktGroupMapper;
import com.orangecrave.admin.marketing.mapper.MktGroupMemberMapper;
import com.orangecrave.admin.order.constant.OrderStatus;
import com.orangecrave.admin.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼团（见 04 篇 §3.2）。3 人成团，24 小时；失败自动退款。
 * 成团判定与退款并发由订单侧条件 UPDATE 保证。
 */
@Service
@RequiredArgsConstructor
public class MktGroupService {

    public static final int GROUPING = 1;
    public static final int SUCCESS = 2;
    public static final int FAILED = 3;

    private final MktGroupMapper groupMapper;
    private final MktGroupMemberMapper memberMapper;
    private final OrderService orderService;

    public PageResult<MktGroup> page(PageQuery query, Integer status) {
        Page<MktGroup> page = groupMapper.selectPage(query.toPage(), new LambdaQueryWrapper<MktGroup>()
                .eq(status != null, MktGroup::getStatus, status)
                .orderByDesc(MktGroup::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public MktGroup get(Long id) {
        MktGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new BizException("拼团不存在");
        }
        return group;
    }

    public List<MktGroupMember> members(Long groupId) {
        return memberMapper.selectList(new LambdaQueryWrapper<MktGroupMember>()
                .eq(MktGroupMember::getGroupId, groupId));
    }

    /**
     * 到期收口（00 篇 §5「拼团未成团 → 拼团失败 + 自动退款」、03 篇 §7.1）。
     * 满员则成团并把成员订单转待审核；未满则判失败退款。
     * 团状态用条件 UPDATE 抢占，重复触发（延时队列 + 兜底扫描）只会执行一次副作用。 */
    @Transactional(rollbackFor = Exception.class)
    public void expire(Long id) {
        MktGroup group = groupMapper.selectById(id);
        if (group == null || group.getStatus() != GROUPING) {
            return;
        }
        if (group.getExpireTime() != null && group.getExpireTime().isAfter(LocalDateTime.now())) {
            return;
        }
        boolean full = group.getTargetSize() != null && group.getCurrentSize() != null
                && group.getCurrentSize() >= group.getTargetSize();
        if (full) {
            if (groupMapper.transition(id, GROUPING, SUCCESS) != 1) {
                return;
            }
            for (MktGroupMember member : members(id)) {
                if (member.getOrderId() != null) {
                    orderService.transitionTo(member.getOrderId(), OrderStatus.PENDING_AUDIT, "系统", null, "拼团满员成团");
                }
            }
            return;
        }
        if (groupMapper.transition(id, GROUPING, FAILED) != 1) {
            return;
        }
        for (MktGroupMember member : members(id)) {
            if (member.getOrderId() != null) {
                orderService.cancelGroupOrder(member.getOrderId(), "系统", null);
            }
        }
    }

    /** 强制成团（模拟成团，见 02 篇 §2.8）。**/
    @Transactional(rollbackFor = Exception.class)
    @OperLog(module = "拼团", type = "UPDATE", desc = "强制成团")
    public void forceSuccess(Long id, boolean simulated) {
        MktGroup group = get(id);
        if (groupMapper.transition(id, GROUPING, SUCCESS) != 1) {
            throw new BizException("团已结束");
        }
        if (simulated) {
            MktGroup update = new MktGroup();
            update.setId(id);
            update.setIsSimulated(1);
            groupMapper.updateById(update);
        }
        promoteMembers(id);
    }

    /** 强制失败 + 退款。**/
    @Transactional(rollbackFor = Exception.class)
    @OperLog(module = "拼团", type = "UPDATE", desc = "强制失败并退款")
    public void forceFail(Long id) {
        get(id);
        if (groupMapper.transition(id, GROUPING, FAILED) != 1) {
            throw new BizException("团已结束");
        }
        for (MktGroupMember member : members(id)) {
            if (member.getOrderId() != null) {
                orderService.cancelGroupOrder(member.getOrderId(), "系统", null);
            }
        }
    }

    /** 成团后把成员订单从「待成团」推进到「待审核」（04 篇 §3.2）。 */
    private void promoteMembers(Long id) {
        for (MktGroupMember member : members(id)) {
            if (member.getOrderId() != null) {
                orderService.transitionTo(member.getOrderId(), OrderStatus.PENDING_AUDIT, "系统", null, "拼团成团");
            }
        }
    }
}
