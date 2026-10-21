package com.orangecrave.admin.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.goods.entity.PmsFreightTemplate;
import com.orangecrave.admin.goods.mapper.PmsFreightTemplateMapper;
import com.orangecrave.admin.goods.model.dto.PmsFreightTemplateSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PmsFreightTemplateService {

    private final PmsFreightTemplateMapper mapper;

    public PageResult<PmsFreightTemplate> page(PageQuery query) {
        Page<PmsFreightTemplate> page = mapper.selectPage(query.toPage(), new LambdaQueryWrapper<PmsFreightTemplate>()
                .orderByDesc(PmsFreightTemplate::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public void save(PmsFreightTemplateSaveDTO dto) {
        PmsFreightTemplate template = new PmsFreightTemplate();
        template.setId(dto.getId());
        template.setTemplateName(dto.getTemplateName());
        template.setChargeType(dto.getChargeType());
        template.setFirstUnit(dto.getFirstUnit());
        template.setFirstFee(dto.getFirstFee());
        template.setExtraUnit(dto.getExtraUnit());
        template.setExtraFee(dto.getExtraFee());
        template.setFreeThreshold(dto.getFreeThreshold());
        template.setRegionRules(dto.getRegionRules());
        if (template.getId() == null) {
            mapper.insert(template);
        } else {
            mapper.updateById(template);
        }
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
