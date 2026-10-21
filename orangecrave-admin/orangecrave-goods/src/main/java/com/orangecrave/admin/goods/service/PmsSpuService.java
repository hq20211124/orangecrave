package com.orangecrave.admin.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.log.OperLogContext;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.goods.entity.PmsSku;
import com.orangecrave.admin.goods.entity.PmsSpu;
import com.orangecrave.admin.goods.mapper.PmsSkuMapper;
import com.orangecrave.admin.goods.mapper.PmsSpuMapper;
import com.orangecrave.admin.goods.model.dto.PmsSpuSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PmsSpuService {

    private final PmsSpuMapper spuMapper;
    private final PmsSkuMapper skuMapper;
    private final ObjectMapper objectMapper;

    public PageResult<PmsSpu> page(PageQuery query) {
        Page<PmsSpu> page = spuMapper.selectPage(query.toPage(), new LambdaQueryWrapper<PmsSpu>()
                .like(StringUtils.hasText(query.getKeyword()), PmsSpu::getSpuName, query.getKeyword())
                .orderByDesc(PmsSpu::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public PmsSpu get(Long id) {
        PmsSpu spu = spuMapper.selectById(id);
        if (spu == null) {
            throw new BizException("商品不存在");
        }
        return spu;
    }

    public List<PmsSku> skus(Long spuId) {
        return skuMapper.selectList(new LambdaQueryWrapper<PmsSku>().eq(PmsSku::getSpuId, spuId));
    }

    public void save(PmsSpuSaveDTO dto) {
        PmsSpu spu = new PmsSpu();
        spu.setId(dto.getId());
        spu.setSpuName(dto.getSpuName());
        spu.setSubTitle(dto.getSubTitle());
        spu.setCover(dto.getCover());
        spu.setImages(dto.getImages());
        spu.setSeriesId(dto.getSeriesId());
        spu.setCategoryId(dto.getCategoryId());
        spu.setGoodsType(dto.getGoodsType());
        spu.setPricingType(dto.getPricingType());
        spu.setBasePrice(dto.getBasePrice());
        spu.setAreaPrice(dto.getAreaPrice());
        spu.setPricingConfig(dto.getPricingConfig());
        spu.setDesignerId(dto.getDesignerId());
        spu.setMinOrderQty(dto.getMinOrderQty());
        spu.setIncrementQty(dto.getIncrementQty());
        spu.setFreightTemplateId(dto.getFreightTemplateId());
        spu.setNeedProof(dto.getNeedProof());
        spu.setStatus(dto.getStatus());
        if (spu.getId() == null) {
            spuMapper.insert(spu);
        } else {
            spuMapper.updateById(spu);
        }
    }

    public void delete(Long id) {
        spuMapper.deleteById(id);
    }

    public void updateStatus(Long id, Integer status) {
        PmsSpu spu = new PmsSpu();
        spu.setId(id);
        spu.setStatus(status);
        spuMapper.updateById(spu);
    }

    @OperLog(module = "商品", type = "UPDATE", desc = "修改商品价格")
    public void updatePrice(Long id, BigDecimal basePrice, BigDecimal areaPrice) {
        PmsSpu before = get(id);
        OperLogContext.setBefore(toJson(before));
        PmsSpu update = new PmsSpu();
        update.setId(id);
        update.setBasePrice(basePrice);
        update.setAreaPrice(areaPrice);
        spuMapper.updateById(update);
        OperLogContext.setAfter(toJson(get(id)));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
