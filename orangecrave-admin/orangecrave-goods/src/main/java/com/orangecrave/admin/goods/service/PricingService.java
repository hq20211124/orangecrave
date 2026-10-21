package com.orangecrave.admin.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.result.ResultCode;
import com.orangecrave.admin.goods.entity.PmsCraft;
import com.orangecrave.admin.goods.entity.PmsSku;
import com.orangecrave.admin.goods.entity.PmsSpu;
import com.orangecrave.admin.goods.mapper.PmsSkuMapper;
import com.orangecrave.admin.goods.mapper.PmsSpuMapper;
import com.orangecrave.admin.goods.model.PricingRequest;
import com.orangecrave.admin.goods.model.PricingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * 计价（见 04 篇 §2）。三种计价方式 + 工艺加价 + 起订量/递增量校验。
 * 定制品不占库存（见 04 篇 §5）。 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final int AREA_SCALE = 4;
    private static final int MONEY_SCALE = 2;

    private final PmsSpuMapper spuMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsCraftService craftService;
    private final ObjectMapper objectMapper;

    public PricingResult compute(PricingRequest request) {
        PmsSku sku = skuMapper.selectById(request.getSkuId());
        if (sku == null) {
            throw new BizException(ResultCode.GOODS_ERROR, "SKU 不存在");
        }
        PmsSpu spu = spuMapper.selectById(sku.getSpuId());
        if (spu == null) {
            throw new BizException(ResultCode.GOODS_ERROR, "商品不存在");
        }
        return compute(request, sku, spu);
    }

    /**
     * 计价入口（调用方已持有 SKU / SPU 时用这个，省掉两次回查）。
     * 混单下单时订单侧要按行做数量校验、取运费模板，本来就得先把商品查出来；
     * 走这个重载可以让一次下单的 SQL 数不再随行数线性增长。 */
    public PricingResult compute(PricingRequest request, PmsSku sku, PmsSpu spu) {
        if (spu == null) {
            throw new BizException(ResultCode.GOODS_ERROR, "商品不存在");
        }
        if (spu.getStatus() != null && spu.getStatus() == 0) {
            throw new BizException(ResultCode.GOODS_ERROR, "商品已下架");
        }

        int qty = request.getQty() == null ? 0 : request.getQty();
        validateQty(spu, qty);

        BigDecimal area = areaOf(sku);
        PricingResult result = new PricingResult();
        result.setArea(area);
        result.setBasePrice(basePrice(spu, sku, qty, area, request.getActivityUnitPrice()));
        result.setCraftPrice(craftPrice(spu, sku, request, qty, area));
        result.setItemAmount(result.getBasePrice().add(result.getCraftPrice()).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        return result;
    }

    private void validateQty(PmsSpu spu, int qty) {
        if (qty <= 0) {
            throw new BizException(ResultCode.GOODS_ERROR, "数量必须大于 0");
        }
        if (spu.getMinOrderQty() != null && qty < spu.getMinOrderQty()) {
            throw new BizException(ResultCode.GOODS_ERROR, "未满足起订量，还差 " + (spu.getMinOrderQty() - qty) + " 件");
        }
        if (spu.getIncrementQty() != null && spu.getIncrementQty() > 0 && qty % spu.getIncrementQty() != 0) {
            throw new BizException(ResultCode.GOODS_ERROR, "所选数量未满足递增量");
        }
    }

    private BigDecimal basePrice(PmsSpu spu, PmsSku sku, int qty, BigDecimal area, BigDecimal activityUnitPrice) {
        // 04 篇 §4 顺序 1：活动价覆盖原价，工艺加价仍按原规则叠加
        if (activityUnitPrice != null) {
            return activityUnitPrice.multiply(BigDecimal.valueOf(qty)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        int type = spu.getPricingType() == null ? 1 : spu.getPricingType();
        BigDecimal unit;
        switch (type) {
            case 2 -> { // 按面积
                BigDecimal areaPrice = spu.getAreaPrice() == null ? BigDecimal.ZERO : spu.getAreaPrice();
                unit = area.multiply(areaPrice);
            }
            case 3 -> unit = tierPrice(spu, qty);
            default -> unit = sku.getPrice() == null ? BigDecimal.ZERO : sku.getPrice();
        }
        return unit.multiply(BigDecimal.valueOf(qty)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal tierPrice(PmsSpu spu, int qty) {
        if (spu.getPricingConfig() == null || spu.getPricingConfig().isBlank()) {
            return spu.getBasePrice() == null ? BigDecimal.ZERO : spu.getBasePrice();
        }
        try {
            JsonNode tiers = objectMapper.readTree(spu.getPricingConfig());
            BigDecimal chosen = null;
            int chosenMin = -1;
            for (JsonNode tier : tiers) {
                int minQty = tier.path("minQty").asInt(0);
                if (qty >= minQty && minQty >= chosenMin) {
                    chosenMin = minQty;
                    chosen = new BigDecimal(tier.path("price").asText("0"));
                }
            }
            return chosen == null ? (spu.getBasePrice() == null ? BigDecimal.ZERO : spu.getBasePrice()) : chosen;
        } catch (Exception e) {
            throw new BizException(ResultCode.GOODS_ERROR, "阶梯计价配置错误");
        }
    }

    private BigDecimal craftPrice(PmsSpu spu, PmsSku sku, PricingRequest request, int qty, BigDecimal area) {
        List<Long> craftIds = request.getCraftIds();
        if (craftIds == null || craftIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<PmsCraft> crafts = craftService.getByIds(craftIds);
        BigDecimal total = BigDecimal.ZERO;
        for (PmsCraft craft : crafts) {
            BigDecimal addPrice = craft.getAddPrice() == null ? BigDecimal.ZERO : craft.getAddPrice();
            if (craft.getAddPriceType() != null && craft.getAddPriceType() == 2) {
                // 按面积：单价 × 面积 × 数量
                total = total.add(addPrice.multiply(area).multiply(BigDecimal.valueOf(qty)));
            } else {
                // 固定：每件加价
                total = total.add(addPrice.multiply(BigDecimal.valueOf(qty)));
            }
        }
        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal areaOf(PmsSku sku) {
        if (sku.getLengthMm() == null || sku.getWidthMm() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal length = BigDecimal.valueOf(sku.getLengthMm()).divide(THOUSAND, AREA_SCALE, RoundingMode.HALF_UP);
        BigDecimal width = BigDecimal.valueOf(sku.getWidthMm()).divide(THOUSAND, AREA_SCALE, RoundingMode.HALF_UP);
        return length.multiply(width).setScale(AREA_SCALE, RoundingMode.HALF_UP);
    }
}
