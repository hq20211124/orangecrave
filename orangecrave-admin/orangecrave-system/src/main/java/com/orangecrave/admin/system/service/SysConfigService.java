package com.orangecrave.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.cache.Cache;
import com.orangecrave.admin.common.cache.CacheFactory;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.log.OperLogContext;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.system.entity.SysConfig;
import com.orangecrave.admin.system.mapper.SysConfigMapper;
import com.orangecrave.admin.system.model.dto.SysConfigDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 系统参数配置（见 01 篇 §1 / 02 篇 §2.2）。key 对齐顾客端/selectParamsConfigByKey 机制。
 *
 * <p>配置是典型读多写少：下单、审核、看板都会读它。这里按 configKey 缓存；
 * 所有读都走 {@link #getByKey}、所有写都走本 Service，因此把 update/batchUpdate 设为唯一失效点，
 * 不会出现「改了配置某处却读不到」的漏网。
 */
@Service
public class SysConfigService {

    private final SysConfigMapper configMapper;
    private final ObjectMapper objectMapper;
    /** 按 configKey 缓存。具体是本机缓存还是跨实例共享缓存由 CacheFactory 装配决定。*/
    private final Cache<String, SysConfig> cache;

    public SysConfigService(SysConfigMapper configMapper, ObjectMapper objectMapper, CacheFactory cacheFactory) {
        this.configMapper = configMapper;
        this.objectMapper = objectMapper;
        this.cache = cacheFactory.create("sys-config");
    }

    public PageResult<SysConfig> page(PageQuery query) {
        Page<SysConfig> page = configMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysConfig>()
                .like(StringUtils.hasText(query.getKeyword()), SysConfig::getConfigKey, query.getKeyword())
                .orderByDesc(SysConfig::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public SysConfig getByKey(String key) {
        SysConfig config = cache.get(key, k -> configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, k)));
        if (config == null) {
            throw new BizException(404, "配置项不存在");
        }
        return config;
    }

    @OperLog(module = "系统配置", type = "UPDATE", desc = "修改系统参数")
    public void update(SysConfigDTO dto) {
        SysConfig before = configMapper.selectById(dto.getId());
        if (before == null) {
            throw new BizException("配置项不存在");
        }
        if (before.getConfigType() != null && before.getConfigType() == 0) {
            throw new BizException("内置配置不可修改");
        }
        OperLogContext.setBefore(toJson(before));
        SysConfig update = new SysConfig();
        update.setId(dto.getId());
        update.setConfigValue(dto.getConfigValue());
        configMapper.updateById(update);
        cache.invalidate(before.getConfigKey());
        SysConfig after = configMapper.selectById(dto.getId());
        OperLogContext.setAfter(toJson(after));
    }

    @OperLog(module = "系统配置", type = "UPDATE", desc = "批量修改系统参数")
    public void batchUpdate(List<SysConfigDTO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 一次查出修改前快照：原来逐条 selectById，N 条配置就是 N 次数据库往返
        List<Long> ids = list.stream().map(SysConfigDTO::getId)
                .filter(Objects::nonNull).distinct().toList();
        OperLogContext.setBefore(toJson(configMapper.selectBatchIds(ids)));
        for (SysConfigDTO dto : list) {
            SysConfig update = new SysConfig();
            update.setId(dto.getId());
            update.setConfigValue(dto.getConfigValue());
            configMapper.updateById(update);
        }
        // 批量改可能涉及多个键，直接整体失效，避免漏掉某个 key
        cache.invalidateAll();
        OperLogContext.setAfter(toJson(list));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
