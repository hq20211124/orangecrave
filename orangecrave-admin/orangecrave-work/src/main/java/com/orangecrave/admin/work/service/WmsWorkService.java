package com.orangecrave.admin.work.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.log.OperLogContext;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.work.constant.WorkStatus;
import com.orangecrave.admin.work.entity.WmsShareCode;
import com.orangecrave.admin.work.entity.WmsWork;
import com.orangecrave.admin.work.entity.WmsWorkImage;
import com.orangecrave.admin.work.mapper.WmsShareCodeMapper;
import com.orangecrave.admin.work.mapper.WmsWorkImageMapper;
import com.orangecrave.admin.work.mapper.WmsWorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 作品（见 05 篇 §3 状态机）。作品被封禁时，引用它的分享码自动失效。  */
@Service
@RequiredArgsConstructor
public class WmsWorkService {

    public static final String REASON_WORK_BANNED = "关联作品已封禁";

    private final WmsWorkMapper workMapper;
    private final WmsWorkImageMapper workImageMapper;
    private final WmsShareCodeMapper shareCodeMapper;
    private final ObjectMapper objectMapper;

    public PageResult<WmsWork> page(PageQuery query, Integer status) {
        Page<WmsWork> page = workMapper.selectPage(query.toPage(), new LambdaQueryWrapper<WmsWork>()
                .eq(status != null, WmsWork::getStatus, status)
                .like(query.getKeyword() != null, WmsWork::getWorkName, query.getKeyword())
                .orderByDesc(WmsWork::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public WmsWork get(Long id) {
        WmsWork work = workMapper.selectById(id);
        if (work == null) {
            throw new BizException("作品不存在");
        }
        return work;
    }

    public List<WmsWorkImage> images(Long workId) {
        return workImageMapper.selectList(new LambdaQueryWrapper<WmsWorkImage>()
                .eq(WmsWorkImage::getWorkId, workId).orderByAsc(WmsWorkImage::getSlotIndex));
    }

    @Transactional(rollbackFor = Exception.class)
    @OperLog(module = "作品", type = "AUDIT", desc = "强制改作品状态")
    public void updateStatus(Long id, Integer status, String remark) {
        WmsWork before = get(id);
        OperLogContext.setBefore(toJson(before));
        WmsWork update = new WmsWork();
        update.setId(id);
        update.setStatus(status);
        update.setAuditRemark(remark);
        workMapper.updateById(update);
        // 连带：作品违规 封禁 → 引用它的分享码自动失效（见 05 篇 §6.3）
        if (status != null && status == WorkStatus.VIOLATION) {
            List<WmsShareCode> codes = shareCodeMapper.selectList(new LambdaQueryWrapper<WmsShareCode>()
                    .eq(WmsShareCode::getWorkId, id).eq(WmsShareCode::getStatus, 1));
            for (WmsShareCode code : codes) {
                WmsShareCode codeUpdate = new WmsShareCode();
                codeUpdate.setId(code.getId());
                codeUpdate.setStatus(0);
                codeUpdate.setInvalidReason(REASON_WORK_BANNED);
                shareCodeMapper.updateById(codeUpdate);
            }
        }
        OperLogContext.setAfter(toJson(workMapper.selectById(id)));
    }

    /**
     * 作品到期（00 篇 §5「作品过期 → 状态转『作品过期』」）。
     * 只从「正常」转，人工下架/违规的状态由审核侧决定，不被定时任务覆盖。 */
    public void expire(Long id) {
        workMapper.markExpired(id, WorkStatus.NORMAL, WorkStatus.EXPIRED);
    }

    public void delete(Long id) {
        WmsWork work = get(id);
        if (work.getDeletedShared() != null && work.getDeletedShared() == 1) {
            throw new BizException("作品分享中，暂不支持删除");
        }
        workMapper.deleteById(id);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
