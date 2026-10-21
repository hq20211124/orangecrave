package com.orangecrave.admin.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.goods.entity.PmsTemplate;
import com.orangecrave.admin.goods.mapper.PmsTemplateMapper;
import com.orangecrave.admin.goods.model.dto.PmsTemplateSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PmsTemplateService {

    private final PmsTemplateMapper mapper;

    public PageResult<PmsTemplate> page(PageQuery query) {
        Page<PmsTemplate> page = mapper.selectPage(query.toPage(), new LambdaQueryWrapper<PmsTemplate>()
                .like(query.getKeyword() != null, PmsTemplate::getTemplateName, query.getKeyword())
                .orderByDesc(PmsTemplate::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public void save(PmsTemplateSaveDTO dto) {
        PmsTemplate template = new PmsTemplate();
        template.setId(dto.getId());
        template.setTemplateName(dto.getTemplateName());
        template.setDesignerId(dto.getDesignerId());
        template.setCategoryId(dto.getCategoryId());
        template.setPreviewUrl(dto.getPreviewUrl());
        template.setSourceUrl(dto.getSourceUrl());
        template.setStatus(dto.getStatus());
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
