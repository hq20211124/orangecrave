package com.orangecrave.admin.marketing.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MktCouponSaveDTO {

    private Long id;
    private Long activityId;
    @NotBlank(message = "券名称不能为空")
    @Size(max = 100, message = "券名称过长")
    private String couponName;
    /** 1 满减 / 2 折扣 / 3 无门槛 */
    private Integer couponType;
    @DecimalMin(value = "0.00", message = "门槛金额不能为负")
    private BigDecimal threshold;
    @DecimalMin(value = "0.00", message = "面额不能为负")
    private BigDecimal amount;
    @DecimalMin(value = "0.00", message = "折扣率不能为负")
    private BigDecimal discount;
    private Integer validDays;
    @Min(value = 0, message = "发放总量不能为负")
    private Integer totalQty;
}
