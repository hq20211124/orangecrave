package com.orangecrave.admin.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orangecrave.admin.goods.entity.PmsCategory;
import com.orangecrave.admin.goods.mapper.PmsCategoryMapper;
import com.orangecrave.admin.goods.model.dto.PmsCategorySaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PmsCategoryService {

    private final PmsCategoryMapper mapper;

    /** 扁平列表（前端自行组树） */
    public List<PmsCategory> list() {
        return mapper.selectList(new LambdaQueryWrapper<PmsCategory>().orderByAsc(PmsCategory::getSort));
    }

    /** 树形结构 */
    public List<PmsCategory> tree() {
        List<PmsCategory> all = list();
        List<PmsCategory> roots = new ArrayList<>();
        for (PmsCategory node : all) {
            if (node.getParentId() == null || node.getParentId() == 0) {
                roots.add(node);
            }
        }
        return roots;
    }

    public PmsCategory get(Long id) {
        return mapper.selectById(id);
    }

    public void save(PmsCategorySaveDTO dto) {
        PmsCategory category = new PmsCategory();
        category.setId(dto.getId());
        category.setCategoryName(dto.getCategoryName());
        category.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        category.setSort(dto.getSort());
        category.setStatus(dto.getStatus());
        if (category.getId() == null) {
            mapper.insert(category);
        } else {
            mapper.updateById(category);
        }
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
