package com.orangecrave.admin.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.log.OperLogContext;
import com.orangecrave.admin.goods.entity.PmsSku;
import com.orangecrave.admin.goods.mapper.PmsSkuMapper;
import com.orangecrave.admin.goods.model.dto.PmsSkuPriceDTO;
import com.orangecrave.admin.goods.model.dto.PmsSkuSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PmsSkuService {

    private final PmsSkuMapper skuMapper;
    private final ObjectMapper objectMapper;

    public List<PmsSku> list(Long spuId) {
        return skuMapper.selectList(new LambdaQueryWrapper<PmsSku>().eq(PmsSku::getSpuId, spuId));
    }

    public PmsSku get(Long id) {
        PmsSku sku = skuMapper.selectById(id);
        if (sku == null) {
            throw new BizException("SKU 不存在");
        }
        return sku;
    }

    public void save(PmsSkuSaveDTO dto) {
        PmsSku sku = new PmsSku();
        sku.setId(dto.getId());
        sku.setSpuId(dto.getSpuId());
        sku.setSkuCode(dto.getSkuCode());
        sku.setSpecValueIds(dto.getSpecValueIds());
        sku.setPrice(dto.getPrice());
        sku.setStock(dto.getStock());
        sku.setWeight(dto.getWeight());
        sku.setImage(dto.getImage());
        sku.setLengthMm(dto.getLengthMm());
        sku.setWidthMm(dto.getWidthMm());
        sku.setStatus(dto.getStatus());
        if (sku.getId() == null) {
            skuMapper.insert(sku);
        } else {
            skuMapper.updateById(sku);
        }
    }

    public void delete(Long id) {
        skuMapper.deleteById(id);
    }

    @OperLog(module = "商品", type = "UPDATE", desc = "修改库存")
    public void updateStock(Long id, Integer stock) {
        PmsSku before = get(id);
        OperLogContext.setBefore(toJson(before));
        PmsSku update = new PmsSku();
        update.setId(id);
        update.setStock(stock);
        skuMapper.updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    @OperLog(module = "商品", type = "UPDATE", desc = "批量改价")
    public void batchPrice(List<PmsSkuPriceDTO> items) {
        OperLogContext.setBefore(toJson(items));
        for (PmsSkuPriceDTO item : items) {
            PmsSku update = new PmsSku();
            update.setId(item.getId());
            update.setPrice(item.getPrice());
            skuMapper.updateById(update);
        }
    }

    public void updatePrice(Long id, BigDecimal price) {
        PmsSku sku = new PmsSku();
        sku.setId(id);
        sku.setPrice(price);
        skuMapper.updateById(sku);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
