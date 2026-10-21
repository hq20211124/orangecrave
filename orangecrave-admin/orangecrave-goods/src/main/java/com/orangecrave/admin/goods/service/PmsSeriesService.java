package com.orangecrave.admin.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.goods.entity.PmsSeries;
import com.orangecrave.admin.goods.mapper.PmsSeriesMapper;
import com.orangecrave.admin.goods.model.dto.PmsSeriesSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PmsSeriesService {

    private final PmsSeriesMapper mapper;

    public PageResult<PmsSeries> page(PageQuery query) {
        Page<PmsSeries> page = mapper.selectPage(query.toPage(), new LambdaQueryWrapper<PmsSeries>()
                .like(StringUtils.hasText(query.getKeyword()), PmsSeries::getSeriesName, query.getKeyword())
                .orderByAsc(PmsSeries::getSort));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public List<PmsSeries> list() {
        return mapper.selectList(new LambdaQueryWrapper<PmsSeries>()
                .eq(PmsSeries::getStatus, 1).orderByAsc(PmsSeries::getSort));
    }

    public PmsSeries get(Long id) {
        return mapper.selectById(id);
    }

    public void save(PmsSeriesSaveDTO dto) {
        PmsSeries series = new PmsSeries();
        series.setId(dto.getId());
        series.setSeriesName(dto.getSeriesName());
        series.setCover(dto.getCover());
        series.setIpName(dto.getIpName());
        series.setSort(dto.getSort());
        series.setStatus(dto.getStatus());
        if (series.getId() == null) {
            mapper.insert(series);
        } else {
            mapper.updateById(series);
        }
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }
}
