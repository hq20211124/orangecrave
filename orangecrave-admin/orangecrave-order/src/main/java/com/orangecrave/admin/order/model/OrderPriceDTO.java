package com.orangecrave.admin.order.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员改价入参（见 02 篇 §2.6 order/{id}/price）。未传的字段保持原值；
 * 未传 payAmount 时按 商品金额 + 运费 - 优惠 重算。 */
@Data
public class OrderPriceDTO {

    @DecimalMin(value = "0.00", message = "金额不能为负")
    private BigDecimal totalAmount;
    @DecimalMin(value = "0.00", message = "金额不能为负")
    private BigDecimal freightAmount;
    @DecimalMin(value = "0.00", message = "金额不能为负")
    private BigDecimal discountAmount;
    @DecimalMin(value = "0.00", message = "金额不能为负")
    private BigDecimal payAmount;
    /** 改价原因（记入操作日志） */
    @Size(max = 500, message = "改价原因过长")
    private String reason;
}
