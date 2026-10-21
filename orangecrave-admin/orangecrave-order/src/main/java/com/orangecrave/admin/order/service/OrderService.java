package com.orangecrave.admin.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.audit.service.AuditService;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.log.OperLogContext;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.text.SensitiveWordChecker;
import com.orangecrave.admin.finance.entity.RefundLedger;
import com.orangecrave.admin.finance.service.FinRefundService;
import com.orangecrave.admin.goods.entity.PmsSku;
import com.orangecrave.admin.goods.entity.PmsSpu;
import com.orangecrave.admin.goods.mapper.PmsSkuMapper;
import com.orangecrave.admin.goods.mapper.PmsSpuMapper;
import com.orangecrave.admin.goods.model.PricingRequest;
import com.orangecrave.admin.goods.model.PricingResult;
import com.orangecrave.admin.goods.service.FreightService;
import com.orangecrave.admin.goods.service.PricingService;
import com.orangecrave.admin.order.adapter.MarketingAdapter;
import com.orangecrave.admin.order.adapter.PaymentAdapter;
import com.orangecrave.admin.order.adapter.ProductionAdapter;
import com.orangecrave.admin.order.adapter.RefundAdapter;
import com.orangecrave.admin.order.constant.MesStatus;
import com.orangecrave.admin.order.constant.OrderStatus;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.entity.OmsOrderImage;
import com.orangecrave.admin.order.entity.OmsOrderItem;
import com.orangecrave.admin.order.entity.OmsOrderStatusLog;
import com.orangecrave.admin.order.mapper.OmsOrderImageMapper;
import com.orangecrave.admin.order.mapper.OmsOrderItemMapper;
import com.orangecrave.admin.order.mapper.OmsOrderMapper;
import com.orangecrave.admin.order.mapper.OmsOrderStatusLogMapper;
import com.orangecrave.admin.order.model.ImageSlot;
import com.orangecrave.admin.order.model.OrderItemInput;
import com.orangecrave.admin.order.model.OrderPreviewDTO;
import com.orangecrave.admin.order.model.OrderPreviewVO;
import com.orangecrave.admin.order.model.OrderPriceDTO;
import com.orangecrave.admin.order.model.OrderSubmitDTO;
import com.orangecrave.admin.system.entity.SysConfig;
import com.orangecrave.admin.system.service.SysConfigService;
import com.orangecrave.admin.task.engine.DelayTaskEngine;
import com.orangecrave.admin.work.entity.WmsShareCode;
import com.orangecrave.admin.work.service.WmsShareCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单核心（见 03 篇）。所有流转走条件 UPDATE 保证并发正确与幂等。  */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final String TASK_UNPAID_CANCEL = "ORDER_UNPAID_CANCEL";
    public static final String TASK_DELIVERY_ALERT = "ORDER_DELIVERY_ALERT";

    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper itemMapper;
    private final OmsOrderImageMapper imageMapper;
    private final OmsOrderStatusLogMapper statusLogMapper;
    private final PmsSpuMapper spuMapper;
    private final PmsSkuMapper skuMapper;
    private final PricingService pricingService;
    private final FreightService freightService;
    private final SysConfigService configService;
    private final DelayTaskEngine taskEngine;
    private final PaymentAdapter paymentAdapter;
    private final ProductionAdapter productionAdapter;
    private final RefundAdapter refundAdapter;
    private final FinRefundService finRefundService;
    private final WmsShareCodeService shareCodeService;
    private final MarketingAdapter marketingAdapter;
    private final SensitiveWordChecker wordChecker;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /** 计价预览（不落库）：整单聚合 + 逐行明细（03 篇 §1.3）。**/
    public OrderPreviewVO preview(OrderPreviewDTO dto) {
        QuoteResult result = quote(normalizeItems(dto.getItems(), dto.getSkuId(), dto.getQty(), dto.getCraftIds(),
                dto.getActivityId(), null), dto.getUserId(), dto.getCouponUserId());
        OrderPreviewVO vo = new OrderPreviewVO();
        BeanUtils.copyProperties(result.aggregate, vo);
        vo.setLines(result.lines.stream().map(OrderService::toPreviewLine).toList());
        return vo;
    }

    private static OrderPreviewVO.Line toPreviewLine(LineQuote line) {
        OrderPreviewVO.Line vo = new OrderPreviewVO.Line();
        vo.setSkuId(line.sku.getId());
        vo.setSpuId(line.spu.getId());
        vo.setGoodsName(line.spu.getSpuName());
        vo.setImage(line.sku.getImage());
        vo.setQty(line.qty);
        vo.setArea(line.priced.getArea());
        vo.setBasePrice(line.priced.getBasePrice());
        vo.setCraftPrice(line.priced.getCraftPrice());
        vo.setItemAmount(line.priced.getItemAmount());
        return vo;
    }

    /**
     * 混单归一（03 篇 §1.3）：传了 items 以它为准；否则把单行简写字段（老调用方：
     * 小程序确认页、管理端代客下单）组装成一行，零改动兼容。  */
    private List<OrderItemInput> normalizeItems(List<OrderItemInput> items, Long skuId, Integer qty,
                                                List<Long> craftIds, Long activityId, List<ImageSlot> images) {
        if (items != null && !items.isEmpty()) {
            return items;
        }
        OrderItemInput single = new OrderItemInput();
        single.setSkuId(skuId);
        single.setQty(qty);
        single.setCraftIds(craftIds);
        single.setActivityId(activityId);
        single.setImages(images);
        return List.of(single);
    }

    /**
     * 04 篇 §2.3 起订量与递增量：低于起订量不能下单（提示还差多少），
     * 数量须为递增量整数倍。两步都按行校验，混单里每行独立。  */
    private void validateQty(PmsSpu spu, Integer qty) {
        if (qty == null || qty < 1) {
            throw new BizException("数量必须大于 0");
        }
        int min = spu.getMinOrderQty() == null ? 1 : spu.getMinOrderQty();
        if (qty < min) {
            throw new BizException("「" + spu.getSpuName() + "」起订量为 " + min + " 件，还差 " + (min - qty) + " 件");
        }
        int increment = spu.getIncrementQty() == null ? 1 : spu.getIncrementQty();
        if (increment > 1 && qty % increment != 0) {
            throw new BizException("「" + spu.getSpuName() + "」数量须为 " + increment + " 的整数倍");
        }
    }

    /** 一行计价结果：行级计价 + 行上的商品快照（落明细用）。 */
    private static class LineQuote {
        private final PmsSku sku;
        private final PmsSpu spu;
        private final int qty;
        private final PricingResult priced;

        private LineQuote(PmsSku sku, PmsSpu spu, int qty, PricingResult priced) {
            this.sku = sku;
            this.spu = spu;
            this.qty = qty;
            this.priced = priced;
        }
    }

    /** 整单计价结果：聚合金额 + 逐行明细。 */
    private static class QuoteResult {
        private final PricingResult aggregate;
        private final List<LineQuote> lines;

        private QuoteResult(PricingResult aggregate, List<LineQuote> lines) {
            this.aggregate = aggregate;
            this.lines = lines;
        }
    }

    /**
     * 整单计价编排（04 篇 §4 顺序，逐行算商品金额后整单聚合）：
     * 活动价覆盖原价 → 优惠券抵扣 → 满额包邮判定 → 运费模板。
     * 满额包邮的判定基数是券前商品金额，不是应付金额；混单运费按整单算，
     * 运费模板取第一行的（03 篇 §1.3「运费按整单计算」）。  */
    private QuoteResult quote(List<OrderItemInput> lines, Long userId, Long couponUserId) {
        if (lines == null || lines.isEmpty()) {
            throw new BizException("请选择商品");
        }
        // 整单涉及的 SKU / SPU 一次查出来。原来每行各查一遍，且与计价、营销内部的回查重复：
        // 3 行混单要 6 次商品查询 + 3 次 SKU 回查；批量预加载后固定为 2 次。
        Map<Long, PmsSku> skuMap = loadSkus(lines);
        Map<Long, PmsSpu> spuMap = loadSpus(skuMap);
        BigDecimal itemAmount = BigDecimal.ZERO;
        int totalQty = 0;
        Long freightTemplateId = null;
        boolean hasActivityPrice = false;
        // 同一活动 + 同一 SKU 只问营销一次（混单里多行同活动很常见）
        Map<String, BigDecimal> activityPriceCache = new LinkedHashMap<>();
        List<LineQuote> lineQuotes = new ArrayList<>(lines.size());
        for (OrderItemInput line : lines) {
            PmsSku sku = skuMap.get(line.getSkuId());
            if (sku == null) {
                throw new BizException("SKU 不存在");
            }
            PmsSpu spu = spuMap.get(sku.getSpuId());
            if (spu == null) {
                throw new BizException("商品不存在");
            }
            validateQty(spu, line.getQty());
            if (freightTemplateId == null) {
                freightTemplateId = spu.getFreightTemplateId();
            }
            PricingRequest request = new PricingRequest();
            request.setSkuId(sku.getId());
            request.setQty(line.getQty());
            request.setCraftIds(line.getCraftIds());
            BigDecimal activityUnitPrice = activityUnitPrice(line.getActivityId(), sku, spu, activityPriceCache);
            if (activityUnitPrice != null) {
                hasActivityPrice = true;
            }
            request.setActivityUnitPrice(activityUnitPrice);
            PricingResult priced = pricingService.compute(request, sku, spu);
            itemAmount = itemAmount.add(priced.getItemAmount());
            totalQty += line.getQty();
            lineQuotes.add(new LineQuote(sku, spu, line.getQty(), priced));
        }
        BigDecimal discount = marketingAdapter.couponDeduction(couponUserId, userId, itemAmount, hasActivityPrice);
        BigDecimal freight = freightService.compute(freightTemplateId, itemAmount, totalQty);
        PricingResult aggregate = new PricingResult();
        aggregate.setItemAmount(itemAmount);
        aggregate.setDiscountAmount(discount);
        aggregate.setFreight(freight);
        aggregate.setPayAmount(itemAmount.subtract(discount).add(freight).setScale(2, RoundingMode.HALF_UP));
        // 行级字段（面积/基础价/工艺加价）只在单行时直通，保持老调用方的响应契约；
        // 混单时它们是行的属性，整单没有意义，留空由调用方看 items
        if (lineQuotes.size() == 1) {
            PricingResult line = lineQuotes.get(0).priced;
            aggregate.setArea(line.getArea());
            aggregate.setBasePrice(line.getBasePrice());
            aggregate.setCraftPrice(line.getCraftPrice());
        }
        return new QuoteResult(aggregate, lineQuotes);
    }

    /** 批量取 SKU；用 selectBatchIds 让查询数不随行数增长。 */
    private Map<Long, PmsSku> loadSkus(List<OrderItemInput> lines) {
        List<Long> ids = lines.stream().map(OrderItemInput::getSkuId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, PmsSku> map = new LinkedHashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        List<PmsSku> skus = skuMapper.selectBatchIds(ids);
        if (skus != null) {
            for (PmsSku sku : skus) {
                map.put(sku.getId(), sku);
            }
        }
        return map;
    }

    /** 批量取 SKU 对应的 SPU，同样一次查完。 */
    private Map<Long, PmsSpu> loadSpus(Map<Long, PmsSku> skuMap) {
        List<Long> ids = skuMap.values().stream().map(PmsSku::getSpuId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, PmsSpu> map = new LinkedHashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        List<PmsSpu> spus = spuMapper.selectBatchIds(ids);
        if (spus != null) {
            for (PmsSpu spu : spus) {
                map.put(spu.getId(), spu);
            }
        }
        return map;
    }

    /** 活动单价，同活动 + 同 SKU 在一次计价内只问营销一次。 */
    private BigDecimal activityUnitPrice(Long activityId, PmsSku sku, PmsSpu spu,
                                         Map<String, BigDecimal> cache) {
        if (activityId == null) {
            return null;
        }
        String key = activityId + ":" + sku.getId();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        BigDecimal price = marketingAdapter.activityUnitPrice(activityId, sku.getId(), spu.getId());
        cache.put(key, price);
        return price;
    }

    @Transactional(rollbackFor = Exception.class)
    public OmsOrder submit(OrderSubmitDTO dto) {
        List<OrderItemInput> lines = normalizeItems(dto.getItems(), dto.getSkuId(), dto.getQty(),
                dto.getCraftIds(), dto.getActivityId(), dto.getImages());
        // 07 篇 §4.2：带图稿的定制单必须先确认版权承诺（混单里任一行带图即算）
        boolean hasImages = lines.stream().anyMatch(l -> l.getImages() != null && !l.getImages().isEmpty());
        if (hasImages && (dto.getPromiseVersion() == null || dto.getPromiseVersion().isBlank())) {
            throw new BizException("请先确认版权承诺");
        }

        QuoteResult quote = quote(lines, dto.getUserId(), dto.getCouponUserId());
        PricingResult priced = quote.aggregate;
        // 04 篇 §3.3：秒杀抢的是活动独立配额，与 SKU 库存分离；抢不到就在建单前失败。按行抢
        for (OrderItemInput line : lines) {
            marketingAdapter.reserveSeckillStock(line.getActivityId(), dto.getUserId(), line.getQty());
        }
        // 03 篇 §7.3：凭分享码领取 = 下单即核销套数与分享者预付的制品费（套数按整单件数）
        int totalQty = quote.lines.stream().mapToInt(l -> l.qty).sum();
        WmsShareCode shareCode = null;
        if (dto.getShareCode() != null && !dto.getShareCode().isBlank()) {
            shareCode = shareCodeService.redeem(dto.getShareCode().trim(), totalQty);
            if (shareCode.getPayerType() != null && shareCode.getPayerType() == 2) {
                // 无料分享：商品费由分享者预付，领取者只付邮费
                priced.setItemAmount(BigDecimal.ZERO);
                priced.setDiscountAmount(BigDecimal.ZERO);
                priced.setPayAmount(priced.getFreight());
            }
        }

        int unpaidMinutes = configInt("orderUnpaidMinutes", 30);

        OmsOrder order = new OmsOrder();
        order.setOrderNo(generateNo("SO"));
        order.setUserId(dto.getUserId());
        order.setOrderType(shareCode != null && shareCode.getShareType() != null && shareCode.getShareType() == 2 ? 3 : 1);
        order.setStatus(OrderStatus.UNPAID);
        order.setPayerUserId(dto.getUserId());
        order.setTotalAmount(priced.getItemAmount());
        order.setFreightAmount(priced.getFreight());
        order.setDiscountAmount(priced.getDiscountAmount());
        order.setPayAmount(priced.getPayAmount());
        order.setRefundAmount(BigDecimal.ZERO);
        order.setCouponId(dto.getCouponUserId());
        // 整单活动取第一行带活动的（拼团/活动价按整单叙事；秒杀按行，见 item.activity_id）
        order.setActivityId(lines.stream()
                .map(OrderItemInput::getActivityId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null));
        order.setGroupId(dto.getGroupId());
        order.setChannelId(dto.getChannelId());
        if (shareCode != null) {
            order.setShareCodeId(shareCode.getId());
        }
        order.setExpireTime(LocalDateTime.now().plusMinutes(unpaidMinutes));
        order.setReceiverSnapshot(dto.getReceiverSnapshot());
        orderMapper.insert(order);
        // 04 篇 §3.4：提交即锁券，支付核销、取消/退款返还
        marketingAdapter.lockCoupon(dto.getCouponUserId(), dto.getUserId(), order.getId());

        for (int i = 0; i < lines.size(); i++) {
            OrderItemInput line = lines.get(i);
            LineQuote lq = quote.lines.get(i);
            OmsOrderItem item = new OmsOrderItem();
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setSpuId(lq.spu.getId());
            item.setSkuId(lq.sku.getId());
            item.setGoodsName(lq.spu.getSpuName());
            item.setSkuSpec(lq.sku.getSkuCode());
            item.setImage(lq.sku.getImage());
            item.setActivityId(line.getActivityId());
            item.setQty(lq.qty);
            item.setUnitPrice(lq.priced.getBasePrice()
                    .divide(BigDecimal.valueOf(lq.qty), 2, RoundingMode.HALF_UP));
            item.setTotalPrice(lq.priced.getItemAmount());
            item.setCraftSnapshot(toJson(line.getCraftIds()));
            item.setPricingSnapshot(toJson(lq.priced));
            itemMapper.insert(item);

            if (line.getImages() != null) {
                for (ImageSlot slot : line.getImages()) {
                    OmsOrderImage image = new OmsOrderImage();
                    image.setOrderItemId(item.getId());
                    image.setSlotIndex(slot.getSlotIndex());
                    image.setImageUrl(slot.getImageUrl());
                    image.setCopies(slot.getCopies() == null ? 1 : slot.getCopies());
                    image.setPHash(slot.getPHash());
                    imageMapper.insert(image);
                }
            }
        }

        if (dto.getPromiseVersion() != null && !dto.getPromiseVersion().isBlank()) {
            // 05 篇 §2.4：订单再留一条承诺记录，作为「用户承诺过」的凭证
            auditService.recordPromise(dto.getUserId(), AuditService.PROMISE_BIZ_CUSTOM_IMAGE,
                    order.getId(), dto.getPromiseVersion(), null);
        }
        logStatus(order.getId(), null, OrderStatus.UNPAID, "用户", dto.getUserId(), "创建订单");        // 注册待付款超时取消（00 篇 §5.3 ①），兜底由 handler.scanOverdue 兜住
        taskEngine.schedule(TASK_UNPAID_CANCEL, order.getId(), unpaidMinutes * 60_000L);
        return order;
    }

    /** 支付（走支付桩，见 03 篇 §10）。**/
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long orderId) {
        OmsOrder order = get(orderId);
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw new BizException("订单状态不可支付");
        }
        paymentAdapter.pay(order);
        int to = order.getGroupId() != null ? OrderStatus.PENDING_GROUP : OrderStatus.PENDING_AUDIT;
        if (!transition(order, to, "用户", order.getUserId(), "支付成功")) {
            throw new BizException("订单状态已变更，支付回调作废");
        }
        OmsOrder update = new OmsOrder();
        update.setId(orderId);
        update.setPayTime(LocalDateTime.now());
        update.setPayType(1);
        update.setTransactionId("STUB" + System.currentTimeMillis());
        orderMapper.updateById(update);
        // 04 篇 §3.4：支付成功核销优惠券
        marketingAdapter.useCoupon(orderId);
    }

    /** 取消（用户主动或超时任务）。**/
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long orderId, String operatorType, Long operatorId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != OrderStatus.UNPAID) {
            return false;
        }
        boolean ok = transition(order, OrderStatus.CANCELLED, operatorType, operatorId, "取消订单");
        if (ok) {
            // 副作用：返还优惠券 + 归还秒杀配额（04 篇 §5「取消 → 归还」）
            marketingAdapter.releaseCoupon(orderId);
            releaseSeckillStock(order);
        }
        return ok;
    }

    /** 拼团失败：待成团 → 已取消 + 自动退款（见 03 篇 §7.1）。**/
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelGroupOrder(Long orderId, String operatorType, Long operatorId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != OrderStatus.PENDING_GROUP) {
            return false;
        }
        boolean ok = transition(order, OrderStatus.CANCELLED, operatorType, operatorId, "拼团失败，自动退款");
        if (ok) {
            refundAdapter.refund(order, order.getPayAmount(), 2, "拼团失败退款");
            marketingAdapter.releaseCoupon(orderId);
            releaseSeckillStock(order);
        }
        return ok;
    }

    /** 审核放行：待审核 → 待生产。 */
    @Transactional(rollbackFor = Exception.class)
    public boolean auditPass(Long orderId, Long operatorId) {
        OmsOrder order = get(orderId);
        boolean ok = transition(order, OrderStatus.PENDING_PRODUCTION, "管理员", operatorId, "审核通过");
        if (ok) {
            productionAdapter.createWorkOrder(order);
        }
        return ok;
    }

    /** 审核驳回：待审核 → 已取消（回用户端待修改）。 */
    @Transactional(rollbackFor = Exception.class)
    public boolean auditReject(Long orderId, Long operatorId, String remark) {
        OmsOrder order = get(orderId);
        boolean ok = transition(order, OrderStatus.CANCELLED, "管理员", operatorId, remark);
        if (ok) {
            marketingAdapter.releaseCoupon(orderId);
            releaseSeckillStock(order);
        }
        return ok;
    }

    /** 仓管推进生产状态（待生产 → 生产中 → 质检 → 待发货）。**/
    @Transactional(rollbackFor = Exception.class)
    public boolean advanceProduction(Long orderId, Long operatorId) {
        OmsOrder order = get(orderId);
        int to = switch (order.getStatus()) {
            case OrderStatus.PENDING_PRODUCTION -> OrderStatus.PRODUCING;
            case OrderStatus.PRODUCING -> OrderStatus.QC;
            case OrderStatus.QC -> OrderStatus.PENDING_DELIVERY;
            default -> throw new BizException("当前状态不可推进生产");
        };
        boolean ok = transition(order, to, "管理员", operatorId, "推进生产");
        if (ok && to == OrderStatus.PENDING_DELIVERY) {
            // 03 篇 §2.5：进待发货就挂超时告警，只告警不改状态
            taskEngine.schedule(TASK_DELIVERY_ALERT, orderId,
                    pendingDeliveryAlertHours() * 3_600_000L);
        }
        return ok;
    }

    /** 待发货超时告警的阈值（sys_config pendingDeliveryHours，默认 72 小时）。 */
    public int pendingDeliveryAlertHours() {
        return configInt("pendingDeliveryHours", 72);
    }

    /**
     * 待发货超时（00 篇 §5 / 03 篇 §2.5）：只告警，绝不改订单状态。
     * 已发货或已取消的订单再被扫到也直接跳过。 */
    public void alertPendingDelivery(Long orderId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != OrderStatus.PENDING_DELIVERY) {
            return;
        }
        log.warn("订单 {} 已待发货超过 {} 小时，请尽快安排发货", order.getOrderNo(), pendingDeliveryAlertHours());
    }

    /** 发货：待发货 → 已发货。*/
    @Transactional(rollbackFor = Exception.class)
    public boolean deliver(Long orderId, Long operatorId) {
        OmsOrder order = get(orderId);
        return transition(order, OrderStatus.DELIVERED, "管理员", operatorId, "发货");
    }

    /** 确认收货：已发货 → 已完成。*/
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(Long orderId, String operatorType, Long operatorId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != OrderStatus.DELIVERED) {
            return false;
        }
        return transition(order, OrderStatus.COMPLETED, operatorType, operatorId, "确认收货");
    }

    /** 非法订单拦截：→ 状态 13（见 03 篇 §2.1）。*/
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectIllegal(Long orderId, String remark) {
        OmsOrder order = get(orderId);
        boolean ok = transition(order, OrderStatus.ILLEGAL_CANCELLED, "系统", null, remark);
        if (ok) {
            marketingAdapter.releaseCoupon(orderId);
            releaseSeckillStock(order);
        }
        return ok;
    }

    @OperLog(module = "订单", type = "UPDATE", desc = "手工改订单状态")
    @Transactional(rollbackFor = Exception.class)
    public void manualUpdateStatus(Long orderId, Integer to, Long operatorId, String remark) {
        OmsOrder order = get(orderId);
        OperLogContext.setBefore(toJson(order));
        if (!transition(order, to, "管理员", operatorId, remark)) {
            throw new BizException("非法状态流转：" + OrderStatus.name(order.getStatus()) + " → " + OrderStatus.name(to));
        }
        OperLogContext.setAfter(toJson(orderMapper.selectById(orderId)));
    }

    /** 管理员改价（见 02 篇 §2.6）。记改前/改后快照。 */
    @OperLog(module = "订单", type = "UPDATE", desc = "修改订单金额")
    @Transactional(rollbackFor = Exception.class)
    public void updatePrice(Long orderId, OrderPriceDTO dto, Long operatorId) {
        OmsOrder before = get(orderId);
        OperLogContext.setBefore(toJson(before));
        OmsOrder update = new OmsOrder();
        update.setId(orderId);
        if (dto.getTotalAmount() != null) {
            update.setTotalAmount(dto.getTotalAmount());
        }
        if (dto.getFreightAmount() != null) {
            update.setFreightAmount(dto.getFreightAmount());
        }
        if (dto.getDiscountAmount() != null) {
            update.setDiscountAmount(dto.getDiscountAmount());
        }
        BigDecimal total = dto.getTotalAmount() != null ? dto.getTotalAmount() : nvl(before.getTotalAmount());
        BigDecimal freight = dto.getFreightAmount() != null ? dto.getFreightAmount() : nvl(before.getFreightAmount());
        BigDecimal discount = dto.getDiscountAmount() != null ? dto.getDiscountAmount() : nvl(before.getDiscountAmount());
        BigDecimal pay = dto.getPayAmount() != null ? dto.getPayAmount() : total.add(freight).subtract(discount);
        update.setPayAmount(pay);
        orderMapper.updateById(update);
        OperLogContext.setAfter(toJson(orderMapper.selectById(orderId)));
    }

    /** 订单备注（见 02 篇 §2.6 order/{id}/remark）。备注是顾客会看到的文本，先过敏感词。 */
    public void updateRemark(Long orderId, String remark) {
        get(orderId);
        OmsOrder update = new OmsOrder();
        update.setId(orderId);
        update.setRemark(wordChecker.check(remark));
        orderMapper.updateById(update);
    }

    /** 批量导出（发货用，见 02 篇 §2.6 order/batch-export）。v1 返回结构化 JSON，由前端生成文件。 */
    public List<Map<String, Object>> exportForDelivery(List<Long> orderIds) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (orderIds == null) {
            return rows;
        }
        // 订单与明细各一次批量查完：原来逐单 selectById + items()，N 单就是 2N 次数据库往返
        Map<Long, OmsOrder> orderMap = orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(OmsOrder::getId, order -> order));
        Map<Long, List<OmsOrderItem>> itemsByOrder = itemMapper.selectList(
                        new LambdaQueryWrapper<OmsOrderItem>()
                                .in(OmsOrderItem::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(OmsOrderItem::getOrderId));
        // 按传入顺序输出，保证导出行序与用户看到的一致
        for (Long id : orderIds) {
            OmsOrder order = orderMap.get(id);
            if (order == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", order.getId());
            row.put("orderNo", order.getOrderNo());
            row.put("status", order.getStatus());
            row.put("statusName", OrderStatus.name(order.getStatus()));
            row.put("receiverSnapshot", order.getReceiverSnapshot());
            row.put("payAmount", order.getPayAmount());
            row.put("items", itemsByOrder.getOrDefault(order.getId(), List.of()));
            rows.add(row);
        }
        return rows;
    }

    /**
     * 管理员强制退款申请（03 篇 §3.3 两段式第一段）。
     * 只落一条待复核退款流水 + 记操作日志快照，钱要等财务复核通过才动。
     *
     * @return fin_refund_ledger 主键，复核时用这个 id
     */
    @Transactional(rollbackFor = Exception.class)
    @OperLog(module = "订单", type = "FORCE_REFUND", desc = "提交强制退款申请")
    public Long applyForceRefund(Long orderId, BigDecimal amount, String reason, Long operatorId) {
        OmsOrder order = get(orderId);
        if (order.getStatus() == OrderStatus.REFUNDED) {
            throw new BizException("订单已退款，请勿重复操作");
        }
        if (finRefundService.pendingForceReview(orderId) != null) {
            throw new BizException("这张订单已有一笔强制退款申请等待财务复核");
        }
        OperLogContext.setBefore(toJson(order));
        BigDecimal refundAmount = amount != null ? amount
                : nvl(order.getPayAmount()).subtract(nvl(order.getRefundAmount()));
        if (refundAmount.signum() <= 0) {
            throw new BizException("退款金额必须大于 0");
        }
        if (refundAmount.compareTo(nvl(order.getPayAmount())) > 0) {
            throw new BizException("退款金额不能超过实付金额");
        }
        String remark = reason == null || reason.isBlank() ? "管理员强制退款" : reason;
        RefundLedger ledger = finRefundService.recordPendingReview(1, order.getId(), order.getId(),
                order.getPayerUserId() == null ? order.getUserId() : order.getPayerUserId(), refundAmount, remark);
        OperLogContext.setAfter(toJson(orderMapper.selectById(orderId)));
        return ledger.getId();
    }

    /**
     * 强制退款的第二段：财务复核（03 篇 §3.3）。
     * 复核走流水上的条件 UPDATE，两名财务同时点只有一次生效，退款不会执行两遍。
     *
     * @return 渠道退款单号；驳回时返回 null
     */
    @OperLog(module = "订单", type = "FORCE_REFUND", desc = "强制退款财务复核")
    @Transactional(rollbackFor = Exception.class)
    public String reviewForceRefund(Long ledgerId, boolean approved, String reviewRemark, Long reviewerId) {
        RefundLedger ledger = finRefundService.get(ledgerId);
        if (ledger == null) {
            throw new BizException("退款申请不存在");
        }
        if (ledger.getReviewStatus() == null || ledger.getReviewStatus() != 1) {
            throw new BizException("该申请已复核过");
        }
        OmsOrder order = get(ledger.getOrderId());
        OperLogContext.setBefore(toJson(order));
        if (!finRefundService.markReviewed(ledgerId, approved, reviewerId, reviewRemark)) {
            throw new BizException("该申请刚被其他财务处理，请刷新后查看");
        }
        if (!approved) {
            OperLogContext.setAfter(toJson(orderMapper.selectById(order.getId())));
            return null;
        }
        refundAdapter.refundByLedger(ledger);
        OmsOrder update = new OmsOrder();
        update.setId(order.getId());
        update.setRefundAmount(nvl(order.getRefundAmount()).add(nvl(ledger.getAmount())));
        orderMapper.updateById(update);
        if (!transitionForce(order, OrderStatus.REFUNDED, "管理员", reviewerId,
                "强制退款（财务复核通过）：" + ledger.getRemark())) {
            throw new BizException("订单状态已变更，退款申请作废，请刷新后重试");
        }
        marketingAdapter.releaseCoupon(order.getId());
        OperLogContext.setAfter(toJson(orderMapper.selectById(order.getId())));
        return ledger.getRefundNo();
    }

    /** 返还订单占用的优惠券（04 篇 §3.4），供售后等相邻流程复用。 */
    public void releaseCoupon(Long orderId) {
        marketingAdapter.releaseCoupon(orderId);
    }

    /** 归还该订单在秒杀活动上占用的独立配额（04 篇 §3.3），数量取订单明细合计。 */
    private void releaseSeckillStock(OmsOrder order) {
        List<OmsOrderItem> items = items(order.getId());
        // 混单按行归还：同一活动可能占多行，按活动聚合件数
        Map<Long, Integer> qtyByActivity = new LinkedHashMap<>();
        for (OmsOrderItem item : items) {
            if (item.getActivityId() == null) {
                continue;
            }
            qtyByActivity.merge(item.getActivityId(), item.getQty() == null ? 0 : item.getQty(), Integer::sum);
        }
        // 老数据没有行级活动 id 时退回整单活动，保证历史订单也能归还
        if (qtyByActivity.isEmpty() && order.getActivityId() != null) {
            qtyByActivity.put(order.getActivityId(),
                    items.stream().mapToInt(i -> i.getQty() == null ? 0 : i.getQty()).sum());
        }
        qtyByActivity.forEach((activityId, qty) -> {
            if (qty > 0) {
                marketingAdapter.releaseSeckillStock(activityId, qty);
            }
        });
    }

    /** 生产工单分页（待生产/生产中/质检）。 */
    public PageResult<OmsOrder> productionPage(PageQuery query, Integer status) {
        Page<OmsOrder> page = orderMapper.selectPage(query.toPage(), new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getStatus, status == null ? OrderStatus.PENDING_PRODUCTION : status)
                .orderByDesc(OmsOrder::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /** 按订单类型分页（打样单 = order_type 2，见 02 篇 §2.5 wms/proof/page）。 */
    public PageResult<OmsOrder> pageByType(PageQuery query, Integer orderType) {
        Page<OmsOrder> page = orderMapper.selectPage(query.toPage(), new LambdaQueryWrapper<OmsOrder>()
                .eq(orderType != null, OmsOrder::getOrderType, orderType)
                .like(query.getKeyword() != null, OmsOrder::getOrderNo, query.getKeyword())
                .orderByDesc(OmsOrder::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /** 录入 MES 工单号（见 03 篇 §4.4；v1 仓管手工）。 */
    @OperLog(module = "订单", type = "UPDATE", desc = "录入 MES 工单号")
    public void setWorkOrderNo(Long orderId, String workOrderNo, LocalDateTime expectedDelivery) {
        OmsOrder order = get(orderId);
        OperLogContext.setBefore(toJson(order));
        OmsOrder update = new OmsOrder();
        update.setId(orderId);
        update.setWorkOrderNo(workOrderNo);
        update.setExpectedDelivery(expectedDelivery);
        update.setSyncTime(LocalDateTime.now());
        orderMapper.updateById(update);
        OperLogContext.setAfter(toJson(orderMapper.selectById(orderId)));
    }

    /**
     * 从 MES 同步生产状态（03 篇 §4.2 / §4.3）。
     * v1 桩没有回调，只刷同步时间；带 mesStatus 时按映射表落到后台状态，用户看到的仍是粗粒度文案。
     *
     * @return 对用户展示的进度文案，未同步状态时为 null
     */
    @OperLog(module = "订单", type = "UPDATE", desc = "从 MES 同步生产状态")
    @Transactional(rollbackFor = Exception.class)
    public String syncProduction(Long orderId, String mesStatus) {
        OmsOrder order = get(orderId);
        OperLogContext.setBefore(toJson(order));
        productionAdapter.syncStatus(order);
        OmsOrder update = new OmsOrder();
        update.setId(orderId);
        update.setSyncTime(LocalDateTime.now());
        orderMapper.updateById(update);
        if (mesStatus == null || mesStatus.isBlank()) {
            OperLogContext.setAfter(toJson(orderMapper.selectById(orderId)));
            return null;
        }
        MesStatus.Mapping mapping = MesStatus.require(mesStatus);
        int from = order.getStatus();
        if (!transition(order, mapping.orderStatus(), "系统", null, "MES 同步：" + mesStatus.trim())) {
            throw new BizException("订单当前是「" + OrderStatus.name(from) + "」，MES 状态「" + mesStatus.trim() + "」同步不生效");
        }
        OperLogContext.setAfter(toJson(orderMapper.selectById(orderId)));
        return mapping.userText();
    }

    public OmsOrder get(Long orderId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    public PageResult<OmsOrder> page(PageQuery query, Integer status) {
        Page<OmsOrder> page = orderMapper.selectPage(query.toPage(), new LambdaQueryWrapper<OmsOrder>()
                .eq(status != null, OmsOrder::getStatus, status)
                .like(query.getKeyword() != null, OmsOrder::getOrderNo, query.getKeyword())
                .orderByDesc(OmsOrder::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /** 我的订单（顾客端）。**/
    public PageResult<OmsOrder> mine(Long userId, PageQuery query) {
        Page<OmsOrder> page = orderMapper.selectPage(query.toPage(), new LambdaQueryWrapper<OmsOrder>()
                .eq(OmsOrder::getUserId, userId)
                .orderByDesc(OmsOrder::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public List<OmsOrderItem> items(Long orderId) {
        return itemMapper.selectList(new LambdaQueryWrapper<OmsOrderItem>().eq(OmsOrderItem::getOrderId, orderId));
    }

    public List<OmsOrderStatusLog> timeline(Long orderId) {
        return statusLogMapper.selectList(new LambdaQueryWrapper<OmsOrderStatusLog>()
                .eq(OmsOrderStatusLog::getOrderId, orderId).orderByAsc(OmsOrderStatusLog::getId));
    }

    private boolean transition(OmsOrder order, int to, String operatorType, Long operatorId, String remark) {
        int from = order.getStatus();
        // 守卫由 03 篇 §2.3 的流转表生成：非法源状态直接失败，不发 UPDATE、不写副作用
        if (!OrderStatus.canTransition(from, to)) {
            return false;
        }
        int rows = orderMapper.transition(order.getId(), from, to);
        if (rows == 1) {
            logStatus(order.getId(), from, to, operatorType, operatorId, remark);
            order.setStatus(to);
            return true;
        }
        return false;
    }

    /**
     * 绕过流转表的强制流转，仅用于 03 篇 §3.3 的管理员强制退款；仍走条件 UPDATE 保证并发正确。
     * 新代码不要用它。
     */
    private boolean transitionForce(OmsOrder order, int to, String operatorType, Long operatorId, String remark) {
        int from = order.getStatus();
        int rows = orderMapper.transition(order.getId(), from, to);
        if (rows == 1) {
            logStatus(order.getId(), from, to, operatorType, operatorId, remark);
            order.setStatus(to);
            return true;
        }
        return false;
    }

    /** 带守卫的状态流转，供相邻模块（售后等）复用同一条通道。 */
    public boolean transitionTo(Long orderId, int to, String operatorType, Long operatorId, String remark) {
        return transition(get(orderId), to, operatorType, operatorId, remark);
    }

    private void logStatus(Long orderId, Integer from, Integer to, String operatorType, Long operatorId, String remark) {
        OmsOrderStatusLog log = new OmsOrderStatusLog();
        log.setOrderId(orderId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        statusLogMapper.insert(log);
    }

    private int configInt(String key, int fallback) {
        try {
            SysConfig config = configService.getByKey(key);
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_FMT) + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
