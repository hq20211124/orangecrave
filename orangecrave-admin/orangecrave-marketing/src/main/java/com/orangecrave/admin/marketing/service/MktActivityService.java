package com.orangecrave.admin.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.marketing.entity.MktActivity;
import com.orangecrave.admin.marketing.mapper.MktActivityMapper;
import com.orangecrave.admin.marketing.model.dto.MktActivitySaveDTO;
import com.orangecrave.admin.marketing.model.dto.MktSeckillSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MktActivityService {

    private final MktActivityMapper mapper;

    public PageResult<MktActivity> page(PageQuery query, Integer activityType) {
        Page<MktActivity> page = mapper.selectPage(query.toPage(), new LambdaQueryWrapper<MktActivity>()
                .eq(activityType != null, MktActivity::getActivityType, activityType)
                .like(StringUtils.hasText(query.getKeyword()), MktActivity::getActivityName, query.getKeyword())
                .orderByDesc(MktActivity::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public MktActivity get(Long id) {
        MktActivity activity = mapper.selectById(id);
        if (activity == null) {
            throw new BizException("活动不存在");
        }
        return activity;
    }

    public void save(MktActivitySaveDTO dto) {
        MktActivity activity = new MktActivity();
        activity.setId(dto.getId());
        activity.setActivityName(dto.getActivityName());
        activity.setActivityType(dto.getActivityType());
        activity.setSpuIds(dto.getSpuIds());
        activity.setSkuIds(dto.getSkuIds());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setRuleConfig(dto.getRuleConfig());
        activity.setStatus(dto.getStatus());
        if (activity.getId() == null) {
            mapper.insert(activity);
        } else {
            mapper.updateById(activity);
        }
    }

    /** 秒杀与拼团共用 mkt_activity，activity_type 只由服务端固定，不取客户端传值。 */
    public void saveSeckill(MktSeckillSaveDTO dto, Integer activityType) {
        MktActivity activity = new MktActivity();
        activity.setId(dto.getId());
        activity.setActivityName(dto.getActivityName());
        activity.setActivityType(activityType);
        activity.setSpuIds(dto.getSpuIds());
        activity.setSkuIds(dto.getSkuIds());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setRuleConfig(dto.getRuleConfig());
        activity.setStatus(dto.getStatus());
        activity.setTotalStock(dto.getTotalStock());
        if (activity.getId() == null) {
            // 已售只由下单扣减，新建必为 0
            activity.setSoldStock(0);
            mapper.insert(activity);
        } else {
            MktActivity current = get(activity.getId());
            int sold = current.getSoldStock() == null ? 0 : current.getSoldStock();
            if (dto.getTotalStock() != null && dto.getTotalStock() < sold) {
                throw new BizException("秒杀配额不能小于已售 " + sold + " 件");
            }
            mapper.updateById(activity);
        }
    }

    public void updateStatus(Long id, Integer status) {
        MktActivity update = new MktActivity();
        update.setId(id);
        update.setStatus(status);
        mapper.updateById(update);
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
