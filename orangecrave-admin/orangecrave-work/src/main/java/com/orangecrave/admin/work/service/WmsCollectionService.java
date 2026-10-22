package com.orangecrave.admin.work.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.text.SensitiveWordChecker;
import com.orangecrave.admin.work.entity.WmsCollection;
import com.orangecrave.admin.work.entity.WmsCollectionItem;
import com.orangecrave.admin.work.entity.WmsWork;
import com.orangecrave.admin.work.mapper.WmsCollectionItemMapper;
import com.orangecrave.admin.work.mapper.WmsCollectionMapper;
import com.orangecrave.admin.work.mapper.WmsWorkMapper;
import com.orangecrave.admin.work.model.dto.WmsCollectionSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 作品合集（见 01 篇 §4）。规则：2–10 个作品成组。  */
@Service
@RequiredArgsConstructor
public class WmsCollectionService {

    /** 合集成组上下限（01 篇 §4：「至少选中两个」「最多选中10个」） */
    static final int MIN_WORKS = 2;
    static final int MAX_WORKS = 10;

    private final WmsCollectionMapper collectionMapper;
    private final WmsCollectionItemMapper itemMapper;
    private final WmsWorkMapper workMapper;
    private final SensitiveWordChecker wordChecker;

    public PageResult<WmsCollection> page(PageQuery query) {
        Page<WmsCollection> page = collectionMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<WmsCollection>().orderByDesc(WmsCollection::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public WmsCollection get(Long id) {
        WmsCollection collection = collectionMapper.selectById(id);
        if (collection == null) {
            throw new BizException("合集不存在");
        }
        return collection;
    }

    public List<WmsCollectionItem> items(Long collectionId) {
        return itemMapper.selectList(new LambdaQueryWrapper<WmsCollectionItem>()
                .eq(WmsCollectionItem::getCollectionId, collectionId));
    }

    /**
     * 新建/整体替换合集（01 篇 §4）：2–10 个作品、逐个校验存在，合集名过敏感词（05 篇 §2.2）。
     * 明细按「删旧插新」整体替换，避免逐条 diff 带来的半更新状态。 */
    @Transactional(rollbackFor = Exception.class)
    public WmsCollection save(WmsCollectionSaveDTO dto) {
        Set<Long> workIds = new LinkedHashSet<>(dto.getWorkIds() == null ? List.of() : dto.getWorkIds());
        if (workIds.size() < MIN_WORKS) {
            throw new BizException("合集至少选 " + MIN_WORKS + " 个作品");
        }
        if (workIds.size() > MAX_WORKS) {
            throw new BizException("合集最多选 " + MAX_WORKS + " 个作品");
        }
        List<WmsWork> works = workMapper.selectBatchIds(workIds);
        if (works.size() != workIds.size()) {
            throw new BizException("合集里有作品不存在");
        }

        WmsCollection collection = new WmsCollection();
        collection.setId(dto.getId());
        collection.setUserId(dto.getUserId());
        collection.setCollectionName(wordChecker.check(dto.getCollectionName()));
        collection.setCoverUrl(dto.getCoverUrl());
        collection.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (collection.getId() == null) {
            collectionMapper.insert(collection);
        } else {
            get(collection.getId());
            collectionMapper.updateById(collection);
            itemMapper.delete(new LambdaQueryWrapper<WmsCollectionItem>()
                    .eq(WmsCollectionItem::getCollectionId, collection.getId()));
        }
        for (Long workId : workIds) {
            WmsCollectionItem item = new WmsCollectionItem();
            item.setCollectionId(collection.getId());
            item.setWorkId(workId);
            itemMapper.insert(item);
        }
        return collection;
    }

    public void delete(Long id) {
        collectionMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<WmsCollectionItem>()
                .eq(WmsCollectionItem::getCollectionId, id));
    }
}
