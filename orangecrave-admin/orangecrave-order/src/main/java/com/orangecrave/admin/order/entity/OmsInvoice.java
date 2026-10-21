package com.orangecrave.admin.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oms_invoice")
public class OmsInvoice extends BaseEntity {

    private String invoiceNo;
    private Long userId;
    /** 可合并开票的订单 id，逗号分隔 */
    private String orderIds;
    /** 1 个人 / 2 企业 */
    private Integer titleType;
    private String title;
    private String taxNo;
    private BigDecimal amount;
    private String email;
    /** 1 电子 / 2 纸质 */
    private Integer type;
    /** 1 待审 / 2 已开 / 3 驳回 / 4 已取消 */
    private Integer status;
    private String fileUrl;
}
