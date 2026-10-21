package com.orangecrave.admin.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.order.adapter.RefundAdapter;
import com.orangecrave.admin.order.constant.OrderStatus;
import com.orangecrave.admin.order.entity.OmsAftersale;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.mapper.OmsAftersaleMapper;
import com.orangecrave.admin.order.mapper.OmsOrderMapper;
import com.orangecrave.admin.system.entity.SysConfig;
import com.orangecrave.admin.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 售后（见 03 篇 §6）。定制品进入生产后不可退；付款后 4 小时内可自助退款。* */
@Service
@RequiredArgsConstructor
public class AftersaleService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OmsAftersaleMapper aftersaleMapper;
    private final OmsOrderMapper orderMapper;
    private final OrderService orderService;
    private final RefundAdapter refundAdapter;
    private final SysConfigService configService;

    public PageResult<OmsAftersale> page(PageQuery query) {
        Page<OmsAftersale> page = aftersaleMapper.selectPage(query.toPage(), new LambdaQueryWrapper<OmsAftersale>()
                .orderByDesc(OmsAftersale::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public OmsAftersale get(Long id) {
        OmsAftersale aftersale = aftersaleMapper.selectById(id);
        if (aftersale == null) {
            throw new BizException("售后单不存在");
        }
        return aftersale;
    }

    @Transactional(rollbackFor = Exception.class)
    public OmsAftersale apply(Long orderId, Long orderItemId, Integer type, String reason, java.math.BigDecimal amount) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        // 自助退款窗口校验（类型 1 退款）：付款后 N 小时内
        if (type != null && type == 1) {
            int hours = configInt("refundWindowHours", 4);
            if (order.getPayTime() == null || order.getPayTime().plusHours(hours).isBefore(LocalDateTime.now())) {
                throw new BizException(2101, "超出售后窗口：付款后 " + hours + " 小时内方可申请退款");
            }
        }
        OmsAftersale aftersale = new OmsAftersale();
        aftersale.setOrderId(orderId);
        aftersale.setOrderItemId(orderItemId);
        aftersale.setAftersaleNo("AS" + LocalDateTime.now().format(NO_FMT) + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        aftersale.setType(type == null ? 1 : type);
        aftersale.setReason(reason);
        aftersale.setAmount(amount == null ? order.getPayAmount() : amount);
        aftersale.setStatus(1);
        aftersaleMapper.insert(aftersale);
        // 03 篇 §2.3：已发货 申请售后 → 售后中
        if (order.getStatus() == OrderStatus.DELIVERED) {
            orderService.transitionTo(orderId, OrderStatus.AFTERSALE, "用户", order.getUserId(), "申请售后");
        }
        return aftersale;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long operatorId, String remark) {
        OmsAftersale aftersale = get(id);
        if (aftersale.getStatus() != 1) {
            throw new BizException("售后单已处理");
        }
        OmsOrder order = orderMapper.selectById(aftersale.getOrderId());
        String refundNo = refundAdapter.refund(order, aftersale.getAmount(), 4, "售后通过");

        OmsAftersale update = new OmsAftersale();
        update.setId(id);
        update.setStatus(4);
        update.setAuditUserId(operatorId);
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(remark);
        update.setRefundNo(refundNo);
        update.setRefundTime(LocalDateTime.now());
        aftersaleMapper.updateById(update);

        OmsOrder refundUpdate = new OmsOrder();
        refundUpdate.setId(order.getId());
        refundUpdate.setRefundAmount(order.getRefundAmount() == null ? aftersale.getAmount()
                : order.getRefundAmount().add(aftersale.getAmount()));
        orderMapper.updateById(refundUpdate);
        // 03 篇 §2.3：售后中 售后通过 → 已退款
        if (order.getStatus() == OrderStatus.AFTERSALE) {
            orderService.transitionTo(aftersale.getOrderId(), OrderStatus.REFUNDED, "管理员", operatorId, "售后通过退款");
        }
        // 04 篇 §3.4：退款即返还优惠券
        orderService.releaseCoupon(aftersale.getOrderId());
    }

    public void reject(Long id, Long operatorId, String remark) {
        OmsAftersale aftersale = get(id);
        if (aftersale.getStatus() != 1) {
            throw new BizException("售后单已处理");
        }
        OmsAftersale update = new OmsAftersale();
        update.setId(id);
        update.setStatus(3);
        update.setAuditUserId(operatorId);
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(remark);
        aftersaleMapper.updateById(update);
    }

    private int configInt(String key, int fallback) {
        try {
            SysConfig config = configService.getByKey(key);
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
