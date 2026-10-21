package com.orangecrave.admin.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.audit.entity.AudBlacklist;
import com.orangecrave.admin.audit.entity.AudCopyrightPromise;
import com.orangecrave.admin.audit.entity.AudImageAudit;
import com.orangecrave.admin.audit.mapper.AudBlacklistMapper;
import com.orangecrave.admin.audit.mapper.AudCopyrightPromiseMapper;
import com.orangecrave.admin.audit.mapper.AudImageAuditMapper;
import com.orangecrave.admin.audit.util.PHashUtil;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.ResultCode;
import com.orangecrave.admin.common.storage.FileStorage;
import com.orangecrave.admin.system.entity.SysConfig;
import com.orangecrave.admin.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 审核（见 05 篇）。三层：图片审核（pHash 黑名单 + 机器审核桩）、文本审核、作品审核。
 * 机器审核为外部依赖，v1 桩恒通过；pHash 黑名单为真实比对。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    /** aud_copyright_promise.biz_type：1 定制图（见后台 pages/audit/copyright.html 的业务类型映射） */
    public static final int PROMISE_BIZ_CUSTOM_IMAGE = 1;

    private final AudImageAuditMapper imageAuditMapper;
    private final AudBlacklistMapper blacklistMapper;
    private final AudCopyrightPromiseMapper promiseMapper;
    private final FileStorage fileStorage;
    private final SysConfigService configService;

    /**
     * 生效黑名单缓存。黑名单读极多（每次图片上传都要整表比对）、写极少，
     * 原来每次上传都 selectList 全表；缓存后只在第一次和发生变更后查库。
     * 写路径（新增 / 驳回入黑 / 删除）统一调 {@link #invalidateBlacklist()}，保证不会读到旧的。 */
    private volatile List<AudBlacklist> blacklistCache;

    /** 上传即审：pHash 比对黑名单（真实）+ 机器审核（桩，恒通过）。 */
    public AudImageAudit checkImage(String imageKey, int bizType, Long bizId) {
        long hash;
        try (InputStream in = fileStorage.load(imageKey)) {
            hash = PHashUtil.compute(in);
        } catch (Exception e) {
            throw new BizException(ResultCode.AUDIT_ERROR, "图片解析失败: " + e.getMessage());
        }

        int threshold = configInt("audit.pHashThreshold", 5);
        boolean autoReject = configBool("audit.autoRejectOnBlacklist", true);

        AudImageAudit audit = new AudImageAudit();
        audit.setBizType(bizType);
        audit.setBizId(bizId);
        audit.setImageUrl(fileStorage.urlOf(imageKey));
        audit.setPHash(hash);

        if (autoReject && hitsBlacklist(hash, threshold)) {
            audit.setAutoResult(3);
            audit.setAutoRemark("命中违规图库（pHash）");
        } else {
            // 机器审核桩：恒通过（见 05 篇 §2.1 / 03 篇 §10）
            audit.setAutoResult(1);
            audit.setAutoRemark("通过（机器审核桩）");
        }
        imageAuditMapper.insert(audit);
        return audit;
    }

    private boolean hitsBlacklist(long hash, int threshold) {
        for (AudBlacklist item : activeBlacklist()) {
            if (item.getPHash() != null && PHashUtil.hamming(hash, item.getPHash()) <= threshold) {
                return true;
            }
        }
        return false;
    }

    public PageResult<AudImageAudit> page(PageQuery query, Integer autoResult) {
        Page<AudImageAudit> page = imageAuditMapper.selectPage(query.toPage(), new LambdaQueryWrapper<AudImageAudit>()
                .eq(autoResult != null, AudImageAudit::getAutoResult, autoResult)
                .orderByDesc(AudImageAudit::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public AudImageAudit get(Long id) {
        AudImageAudit audit = imageAuditMapper.selectById(id);
        if (audit == null) {
            throw new BizException("审核记录不存在");
        }
        return audit;
    }

    /** 人工放行。*/
    public void approve(Long id, Long operatorId, String remark) {
        updateManual(id, 1, operatorId, remark);
    }

    /** 人工驳回：并入黑名单（pHash），后续相同上传自动拦截。*/
    public void reject(Long id, Long operatorId, String remark) {
        AudImageAudit audit = updateManual(id, 2, operatorId, remark);
        if (audit.getPHash() != null) {
            AudBlacklist blacklist = new AudBlacklist();
            blacklist.setPHash(audit.getPHash());
            blacklist.setSource("人工驳回");
            blacklist.setRemark(remark);
            blacklist.setStatus(1);
            blacklistMapper.insert(blacklist);
            invalidateBlacklist();
        }
    }

    /**
     * 批量审核（见 02 篇 §2.7 image/batch-audit）。result：1 通过 / 2 驳回。
     * 原来逐条走 reject/approve，每条 2 次 SQL（查 + 改），N 条就是 2N 次；
     * 改成一次批量查 + 一次条件 UPDATE，驳回时去重后入黑名单、只清一次缓存。 */
    public int batchAudit(List<Long> ids, int result, String remark, Long operatorId) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        List<AudImageAudit> audits = imageAuditMapper.selectBatchIds(ids);
        if (audits.isEmpty()) {
            return 0;
        }
        List<Long> found = audits.stream().map(AudImageAudit::getId).toList();
        imageAuditMapper.update(null, new LambdaUpdateWrapper<AudImageAudit>()
                .in(AudImageAudit::getId, found)
                .set(AudImageAudit::getManualResult, result)
                .set(AudImageAudit::getAuditUserId, operatorId)
                .set(AudImageAudit::getAuditTime, LocalDateTime.now())
                .set(AudImageAudit::getAuditRemark, remark));
        if (result == 2) {
            List<Long> hashes = audits.stream().map(AudImageAudit::getPHash)
                    .filter(Objects::nonNull).distinct().toList();
            for (Long hash : hashes) {
                AudBlacklist blacklist = new AudBlacklist();
                blacklist.setPHash(hash);
                blacklist.setSource("批量驳回");
                blacklist.setRemark(remark);
                blacklist.setStatus(1);
                blacklistMapper.insert(blacklist);
            }
            if (!hashes.isEmpty()) {
                invalidateBlacklist();
            }
        }
        return found.size();
    }

    /** 上传图片查 pHash 黑名单（见 02 篇 §2.7 blacklist/check）。 */
    public Map<String, Object> checkBlacklist(String imageKey) {
        long hash;
        try (InputStream in = fileStorage.load(imageKey)) {
            hash = PHashUtil.compute(in);
        } catch (Exception e) {
            throw new BizException(ResultCode.AUDIT_ERROR, "图片解析失败: " + e.getMessage());
        }
        int threshold = configInt("audit.pHashThreshold", 5);
        int minDistance = Integer.MAX_VALUE;
        AudBlacklist nearest = null;
        for (AudBlacklist item : activeBlacklist()) {
            if (item.getPHash() == null) {
                continue;
            }
            int distance = PHashUtil.hamming(hash, item.getPHash());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = item;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pHash", hash);
        result.put("threshold", threshold);
        result.put("hit", nearest != null && minDistance <= threshold);
        result.put("nearestDistance", nearest == null ? null : minDistance);
        result.put("matchedId", nearest == null ? null : nearest.getId());
        return result;
    }

    private AudImageAudit updateManual(Long id, int manualResult, Long operatorId, String remark) {
        AudImageAudit audit = get(id);
        AudImageAudit update = new AudImageAudit();
        update.setId(id);
        update.setManualResult(manualResult);
        update.setAuditUserId(operatorId);
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(remark);
        imageAuditMapper.updateById(update);
        audit.setManualResult(manualResult);
        return audit;
    }

    public void addBlacklist(Long pHash, String source, String remark) {
        AudBlacklist blacklist = new AudBlacklist();
        blacklist.setPHash(pHash);
        blacklist.setSource(source);
        blacklist.setRemark(remark);
        blacklist.setStatus(1);
        blacklistMapper.insert(blacklist);
        invalidateBlacklist();
    }

    /** 后台删除黑名单条目；走这里而不是直接调 mapper，缓存才会同步失效。 */
    public void removeBlacklist(Long id) {
        blacklistMapper.deleteById(id);
        invalidateBlacklist();
    }

    /** 生效黑名单（status=1）；带缓存，变动后由 {@link #invalidateBlacklist()} 触发重建。 */
    private List<AudBlacklist> activeBlacklist() {
        List<AudBlacklist> cached = blacklistCache;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (blacklistCache == null) {
                blacklistCache = blacklistMapper.selectList(new LambdaQueryWrapper<AudBlacklist>()
                        .eq(AudBlacklist::getStatus, 1));
            }
            return blacklistCache;
        }
    }

    private void invalidateBlacklist() {
        blacklistCache = null;
    }

    public void recordPromise(Long userId, int bizType, Long bizId, String promiseVersion, String ip) {
        AudCopyrightPromise promise = new AudCopyrightPromise();
        promise.setUserId(userId);
        promise.setBizType(bizType);
        promise.setBizId(bizId);
        promise.setPromiseVersion(promiseVersion);
        promise.setIp(ip);
        promiseMapper.insert(promise);
    }

    public PageResult<AudCopyrightPromise> promisePage(PageQuery query) {
        Page<AudCopyrightPromise> page = promiseMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<AudCopyrightPromise>().orderByDesc(AudCopyrightPromise::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    private int configInt(String key, int fallback) {
        try {
            SysConfig config = configService.getByKey(key);
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private boolean configBool(String key, boolean fallback) {
        try {
            SysConfig config = configService.getByKey(key);
            return Boolean.parseBoolean(config.getConfigValue().trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
