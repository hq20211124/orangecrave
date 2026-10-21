package com.orangecrave.admin.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.goods.entity.PmsDesigner;
import com.orangecrave.admin.goods.mapper.PmsDesignerMapper;
import com.orangecrave.admin.goods.model.dto.PmsDesignerSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 画师：只做归属与统计，不做分成（见 04 篇 §6）。  */
@Service
@RequiredArgsConstructor
public class PmsDesignerService {

    private final PmsDesignerMapper mapper;

    public PageResult<PmsDesigner> page(PageQuery query) {
        Page<PmsDesigner> page = mapper.selectPage(query.toPage(), new LambdaQueryWrapper<PmsDesigner>()
                .like(query.getKeyword() != null, PmsDesigner::getDesignerName, query.getKeyword())
                .orderByDesc(PmsDesigner::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public List<PmsDesigner> list() {
        return mapper.selectList(new LambdaQueryWrapper<PmsDesigner>().eq(PmsDesigner::getStatus, 1));
    }

    public void save(PmsDesignerSaveDTO dto) {
        PmsDesigner designer = new PmsDesigner();
        designer.setId(dto.getId());
        designer.setDesignerName(dto.getDesignerName());
        designer.setAvatar(dto.getAvatar());
        designer.setIntro(dto.getIntro());
        designer.setStatus(dto.getStatus());
        if (designer.getId() == null) {
            mapper.insert(designer);
        } else {
            mapper.updateById(designer);
        }
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
