package com.orangecrave.admin.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.cache.Cache;
import com.orangecrave.admin.common.cache.CacheFactory;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.goods.entity.PmsCraft;
import com.orangecrave.admin.goods.mapper.PmsCraftMapper;
import com.orangecrave.admin.goods.model.dto.PmsCraftSaveDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 工艺字典。写少读多，且计价热路径（见 04 篇 §2）会反复取同一批工艺，
 * 因此全部读都走缓存，save/delete 是唯一失效点。
 */
@Service
public class PmsCraftService {

    /** 启用工艺列表的缓存键（只有一个）。*/
    private static final String ACTIVE_LIST = "active";

    private final PmsCraftMapper mapper;
    private final Cache<String, List<PmsCraft>> activeListCache;
    private final Cache<Long, PmsCraft> byIdCache;

    public PmsCraftService(PmsCraftMapper mapper, CacheFactory cacheFactory) {
        this.mapper = mapper;
        this.activeListCache = cacheFactory.create("pms-craft-list");
        this.byIdCache = cacheFactory.create("pms-craft-id");
    }

    public PageResult<PmsCraft> page(PageQuery query) {
        // 分页维度多、命中率低，不缓存
        Page<PmsCraft> page = mapper.selectPage(query.toPage(), new LambdaQueryWrapper<PmsCraft>()
                .orderByDesc(PmsCraft::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public List<PmsCraft> list() {
        return activeListCache.get(ACTIVE_LIST, k -> mapper.selectList(
                new LambdaQueryWrapper<PmsCraft>().eq(PmsCraft::getStatus, 1)));
    }

    /**
     * 按 id 取工艺（计价热路径）。逐 id 走缓存，未命中的才回库；
     * 同一工艺跨行、跨请求复用，下单时不再随行数增长。
     */
    public List<PmsCraft> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().distinct()
                .map(id -> byIdCache.get(id, mapper::selectById))
                .filter(Objects::nonNull)
                .toList();
    }

    public void save(PmsCraftSaveDTO dto) {
        PmsCraft craft = new PmsCraft();
        craft.setId(dto.getId());
        craft.setCraftName(dto.getCraftName());
        craft.setCraftType(dto.getCraftType());
        craft.setAddPrice(dto.getAddPrice());
        craft.setAddPriceType(dto.getAddPriceType());
        craft.setStatus(dto.getStatus());
        if (craft.getId() == null) {
            mapper.insert(craft);
        } else {
            mapper.updateById(craft);
        }
        invalidate(craft.getId());
    }

    public void delete(Long id) {
        mapper.deleteById(id);
        invalidate(id);
    }

    private void invalidate(Long id) {
        if (id != null) {
            byIdCache.invalidate(id);
        }
        activeListCache.invalidateAll();
    }
}
