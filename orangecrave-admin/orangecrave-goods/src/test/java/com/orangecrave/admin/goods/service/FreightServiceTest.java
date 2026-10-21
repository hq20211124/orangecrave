package com.orangecrave.admin.goods.service;

import com.orangecrave.admin.goods.entity.PmsFreightTemplate;
import com.orangecrave.admin.goods.mapper.PmsFreightTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 运费规则回归（04 篇 §4）。关键口径：满 28 元是「减 4 元运费」，不是整单免运费，续重照收。 */
class FreightServiceTest {

    private final PmsFreightTemplateMapper mapper = Mockito.mock(PmsFreightTemplateMapper.class);
    private final FreightService service = new FreightService(mapper);

    @BeforeEach
    void setUp() {
        PmsFreightTemplate template = new PmsFreightTemplate();
        template.setFirstFee(new BigDecimal("4.00"));
        template.setFirstUnit(1);
        template.setExtraFee(new BigDecimal("1.00"));
        template.setExtraUnit(1);
        template.setFreeThreshold(new BigDecimal("28.00"));
        when(mapper.selectById(1L)).thenReturn(template);
    }

    @Test
    void chargesFirstFeeBelowThreshold() {
        assertThat(service.compute(1L, new BigDecimal("20.00"), 1)).isEqualByComparingTo("4.00");
    }

    @Test
    void singleUnitBecomesFreeAtThreshold() {
        assertThat(service.compute(1L, new BigDecimal("28.00"), 1)).isEqualByComparingTo("0.00");
    }

    @Test
    void extraUnitsStillChargedWhenThresholdMet() {
        // 首件 4 元 + 续重 2 件 × 1 元 = 6 元，减免基础邮费 4 元后仍应收 2 元
        assertThat(service.compute(1L, new BigDecimal("30.00"), 3)).isEqualByComparingTo("2.00");
    }

    @Test
    void missingTemplateChargesNothing() {
        assertThat(service.compute(99L, new BigDecimal("30.00"), 1)).isEqualByComparingTo("0.00");
    }
}
