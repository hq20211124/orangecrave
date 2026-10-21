package com.orangecrave.admin.order.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台发起开票入参（见 01 篇 §5.6）。发票流水号、状态、凭证地址由服务端生成，
 * 分别走 apply / issue·reject / upload 三条路径，不接受外部传入。 */
@Data
public class OmsInvoiceSaveDTO {

    @NotNull(message = "请选择开票用户")
    private Long userId;
    /** 可合并开票的订单 id，逗号分隔 */
    @Size(max = 255, message = "开票订单过长")
    private String orderIds;
    /** 1 个人 / 2 企业 */
    private Integer titleType;
    @NotBlank(message = "发票抬头不能为空")
    @Size(max = 100, message = "发票抬头过长")
    private String title;
    @Size(max = 100, message = "税号过长")
    private String taxNo;
    @DecimalMin(value = "0.00", message = "金额不能为负")
    private BigDecimal amount;
    @Size(max = 100, message = "邮箱过长")
    private String email;
    /** 1 电子 / 2 纸质 */
    private Integer type;
}
