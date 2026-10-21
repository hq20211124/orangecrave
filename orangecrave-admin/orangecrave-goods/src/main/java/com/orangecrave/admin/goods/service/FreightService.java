package com.orangecrave.admin.goods.service;

import com.orangecrave.admin.goods.entity.PmsFreightTemplate;
import com.orangecrave.admin.goods.mapper.PmsFreightTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 运费计算（见 03 篇 §5.2 / 04 篇 §4）。现有规则：基础邮费 4 元/单，满 28 元减 4 元运费。
 * 满额包邮判定基数用「商品金额」（券前）。 */
@Service
@RequiredArgsConstructor
public class FreightService {

    private final PmsFreightTemplateMapper freightMapper;

    public BigDecimal compute(Long templateId, BigDecimal itemAmount, int qty) {
        if (templateId == null) {
            return BigDecimal.ZERO;
        }
        PmsFreightTemplate template = freightMapper.selectById(templateId);
        if (template == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal firstFee = template.getFirstFee() == null ? BigDecimal.ZERO : template.getFirstFee();
        int firstUnit = template.getFirstUnit() == null || template.getFirstUnit() <= 0 ? 1 : template.getFirstUnit();
        BigDecimal extraFee = template.getExtraFee() == null ? BigDecimal.ZERO : template.getExtraFee();
        int extraUnit = template.getExtraUnit() == null || template.getExtraUnit() <= 0 ? 1 : template.getExtraUnit();

        BigDecimal fee = firstFee;
        if (qty > firstUnit) {
            int extraCount = (qty - firstUnit + extraUnit - 1) / extraUnit;
            fee = firstFee.add(extraFee.multiply(BigDecimal.valueOf(extraCount)));
        }
        // 04 篇 §4「满 28 元减 4 元运费」：满额只减免基础邮费，续重部分照收，不是整单免运费
        BigDecimal threshold = template.getFreeThreshold();
        if (threshold != null && itemAmount != null && itemAmount.compareTo(threshold) >= 0) {
            fee = fee.subtract(firstFee).max(BigDecimal.ZERO);
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }
}
