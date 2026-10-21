package com.orangecrave.admin.goods.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.goods.entity.PmsCraft;
import com.orangecrave.admin.goods.entity.PmsSku;
import com.orangecrave.admin.goods.entity.PmsSpu;
import com.orangecrave.admin.goods.mapper.PmsSkuMapper;
import com.orangecrave.admin.goods.mapper.PmsSpuMapper;
import com.orangecrave.admin.goods.model.PricingRequest;
import com.orangecrave.admin.goods.model.PricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 计价单元测试，04 篇 §2。。不依赖数据库。  */
class PricingServiceTest {

    private PmsSpuMapper spuMapper;
    private PmsSkuMapper skuMapper;
    private PmsCraftService craftService;
    private PricingService pricing;

    @BeforeEach
    void setUp() {
        spuMapper = Mockito.mock(PmsSpuMapper.class);
        skuMapper = Mockito.mock(PmsSkuMapper.class);
        craftService = Mockito.mock(PmsCraftService.class);
        pricing = new PricingService(spuMapper, skuMapper, craftService, new ObjectMapper());
    }

    private PricingRequest request(long skuId, int qty) {
        PricingRequest req = new PricingRequest();
        req.setSkuId(skuId);
        req.setQty(qty);
        return req;
    }

    @Test
    void priceByPiece() {
        PmsSpu spu = new PmsSpu();
        spu.setId(1L);
        spu.setPricingType(1);
        spu.setMinOrderQty(10);
        spu.setIncrementQty(10);
        spu.setStatus(1);
        PmsSku sku = new PmsSku();
        sku.setId(1L);
        sku.setSpuId(1L);
        sku.setPrice(new BigDecimal("2.00"));

        when(skuMapper.selectById(1L)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu);

        PricingResult result = pricing.compute(request(1L, 20));
        assertThat(result.getBasePrice()).isEqualByComparingTo("40.00");
        assertThat(result.getItemAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    void priceByArea() {
        PmsSpu spu = new PmsSpu();
        spu.setId(2L);
        spu.setPricingType(2);
        spu.setAreaPrice(new BigDecimal("300.00"));
        spu.setMinOrderQty(1);
        spu.setIncrementQty(1);
        spu.setStatus(1);
        PmsSku sku = new PmsSku();
        sku.setId(2L);
        sku.setSpuId(2L);
        sku.setLengthMm(100);
        sku.setWidthMm(200);

        when(skuMapper.selectById(2L)).thenReturn(sku);
        when(spuMapper.selectById(2L)).thenReturn(spu);

        // 0.1m × 0.2m = 0.02 m²（0.02 × 300 = 6.00）
        PricingResult result = pricing.compute(request(2L, 1));
        assertThat(result.getArea()).isEqualByComparingTo("0.0200");
        assertThat(result.getBasePrice()).isEqualByComparingTo("6.00");
    }

    @Test
    void craftFixedSurchargePerItem() {
        PmsSpu spu = new PmsSpu();
        spu.setId(1L);
        spu.setPricingType(1);
        spu.setMinOrderQty(10);
        spu.setIncrementQty(10);
        spu.setStatus(1);
        PmsSku sku = new PmsSku();
        sku.setId(1L);
        sku.setSpuId(1L);
        sku.setPrice(new BigDecimal("2.00"));

        PmsCraft craft = new PmsCraft();
        craft.setId(2L);
        craft.setAddPrice(new BigDecimal("0.50"));
        craft.setAddPriceType(1);

        when(skuMapper.selectById(1L)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu);
        when(craftService.getByIds(List.of(2L))).thenReturn(List.of(craft));

        PricingRequest req = request(1L, 20);
        req.setCraftIds(List.of(2L));
        PricingResult result = pricing.compute(req);
        assertThat(result.getCraftPrice()).isEqualByComparingTo("10.00");
        assertThat(result.getItemAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void rejectBelowMinOrderQty() {
        PmsSpu spu = new PmsSpu();
        spu.setId(1L);
        spu.setPricingType(1);
        spu.setMinOrderQty(10);
        spu.setIncrementQty(10);
        spu.setStatus(1);
        PmsSku sku = new PmsSku();
        sku.setId(1L);
        sku.setSpuId(1L);
        sku.setPrice(new BigDecimal("2.00"));

        when(skuMapper.selectById(1L)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu);

        assertThatThrownBy(() -> pricing.compute(request(1L, 5)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("起订量");
    }

    @Test
    void rejectBadIncrement() {
        PmsSpu spu = new PmsSpu();
        spu.setId(1L);
        spu.setPricingType(1);
        spu.setMinOrderQty(10);
        spu.setIncrementQty(10);
        spu.setStatus(1);
        PmsSku sku = new PmsSku();
        sku.setId(1L);
        sku.setSpuId(1L);
        sku.setPrice(new BigDecimal("2.00"));

        when(skuMapper.selectById(1L)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu);

        assertThatThrownBy(() -> pricing.compute(request(1L, 15)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("递增量");
    }

    @Test
    void activityUnitPriceOverridesBasePriceButKeepsCraftSurcharge() {
        PmsSpu spu = new PmsSpu();
        spu.setId(1L);
        spu.setPricingType(3);
        spu.setMinOrderQty(10);
        spu.setIncrementQty(10);
        spu.setStatus(1);
        spu.setPricingConfig("[{\"minQty\":1,\"price\":3.00},{\"minQty\":50,\"price\":2.00}]");
        PmsSku sku = new PmsSku();
        sku.setId(1L);
        sku.setSpuId(1L);
        sku.setPrice(new BigDecimal("2.00"));

        PmsCraft craft = new PmsCraft();
        craft.setId(2L);
        craft.setAddPrice(new BigDecimal("0.50"));
        craft.setAddPriceType(1);

        when(skuMapper.selectById(1L)).thenReturn(sku);
        when(spuMapper.selectById(1L)).thenReturn(spu);
        when(craftService.getByIds(List.of(2L))).thenReturn(List.of(craft));

        // 04 篇 §4 顺序 1：活动价（拼团/秒杀）覆盖阶梯价，工艺加价仍叠加
        PricingRequest req = request(1L, 20);
        req.setCraftIds(List.of(2L));
        req.setActivityUnitPrice(new BigDecimal("1.50"));
        PricingResult result = pricing.compute(req);
        assertThat(result.getBasePrice()).isEqualByComparingTo("30.00");
        assertThat(result.getCraftPrice()).isEqualByComparingTo("10.00");
        assertThat(result.getItemAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    void tierPricing() {
        PmsSpu spu = new PmsSpu();
        spu.setId(3L);
        spu.setPricingType(3);
        spu.setMinOrderQty(1);
        spu.setIncrementQty(1);
        spu.setStatus(1);
        spu.setPricingConfig("[{\"minQty\":1,\"price\":3.00},{\"minQty\":50,\"price\":2.00}]");
        PmsSku sku = new PmsSku();
        sku.setId(3L);
        sku.setSpuId(3L);

        when(skuMapper.selectById(3L)).thenReturn(sku);
        when(spuMapper.selectById(3L)).thenReturn(spu);

        assertThat(pricing.compute(request(3L, 10)).getBasePrice()).isEqualByComparingTo("30.00");
        assertThat(pricing.compute(request(3L, 60)).getBasePrice()).isEqualByComparingTo("120.00");
    }
}
