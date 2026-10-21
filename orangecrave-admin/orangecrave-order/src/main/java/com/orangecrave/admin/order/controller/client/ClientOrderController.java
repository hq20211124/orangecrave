package com.orangecrave.admin.order.controller.client;

import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.common.satoken.StpClientUtil;
import com.orangecrave.admin.order.entity.OmsAftersale;
import com.orangecrave.admin.order.entity.OmsOrder;
import com.orangecrave.admin.order.entity.OmsOrderStatusLog;
import com.orangecrave.admin.order.model.OrderPreviewDTO;
import com.orangecrave.admin.order.model.OrderPreviewVO;
import com.orangecrave.admin.order.model.OrderSubmitDTO;
import com.orangecrave.admin.order.model.SeckillSubmitVO;
import com.orangecrave.admin.order.async.AsyncOrderResult;
import com.orangecrave.admin.order.async.AsyncOrderService;
import com.orangecrave.admin.order.service.AftersaleService;
import com.orangecrave.admin.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 顾客端订单接口（见 02 篇 §2.13）。调用方为小程序用户，只能操作自己的数据。  */
@RestController
@RequestMapping("/client-api/oms")
@RequiredArgsConstructor
public class ClientOrderController {

    private final OrderService orderService;
    private final AftersaleService aftersaleService;
    private final AsyncOrderService asyncOrderService;

    @PostMapping("/order/preview")
    public Result<OrderPreviewVO> preview(@Valid @RequestBody OrderPreviewDTO dto) {
        // 券归属校验要真实登录用户，不能信客户端传来的 userId
        dto.setUserId(StpClientUtil.getLoginIdAsLong());
        return Result.ok(orderService.preview(dto));
    }

    @PostMapping("/order/submit")
    public Result<OmsOrder> submit(@Valid @RequestBody OrderSubmitDTO dto) {
        dto.setUserId(StpClientUtil.getLoginIdAsLong());
        return Result.ok(orderService.submit(dto));
    }

    /**
     * 秒杀下单：削峰入口（见 03 篇 §5）。本接口只受理——校验参数、发号、投递消息后立刻返回，
     * 不等待落单；客户端凭 requestId 轮询 {@code /order/seckill/result/{requestId}}。
     * 秒杀是本项目唯一的高并发入口，故只有它走异步；普通下单仍是上面的同步 /order/submit。 */
    @PostMapping("/order/seckill")
    public Result<SeckillSubmitVO> seckillSubmit(@Valid @RequestBody OrderSubmitDTO dto) {
        // 与同步下单同一条口径：用户身份取登录态，不信客户端传的 userId
        dto.setUserId(StpClientUtil.getLoginIdAsLong());
        return Result.ok(new SeckillSubmitVO(asyncOrderService.submit(dto), AsyncOrderResult.QUEUED));
    }

    /**
     * 查询秒杀下单结果：status=QUEUED 继续轮询，SUCCESS 用 orderId 跳订单详情，
     * FAILED 展示 message。data 为 null 表示 requestId 不存在或结果已过期（保留 10 分钟）。 */
    @GetMapping("/order/seckill/result/{requestId}")
    public Result<AsyncOrderResult> seckillResult(@PathVariable String requestId) {
        return Result.ok(asyncOrderService.result(requestId));
    }

    @GetMapping("/order/mine")
    public Result<PageResult<OmsOrder>> mine(PageQuery query) {
        return Result.ok(orderService.mine(StpClientUtil.getLoginIdAsLong(), query));
    }

    @GetMapping("/order/{id}")
    public Result<OmsOrder> detail(@PathVariable Long id) {
        return Result.ok(orderService.get(id));
    }

    @GetMapping("/order/{id}/timeline")
    public Result<List<OmsOrderStatusLog>> timeline(@PathVariable Long id) {
        return Result.ok(orderService.timeline(id));
    }

    /**
     * 支付（演示：走支付桩，见 03 篇 §10）。线上为 wx.requestPayment，此处保留最小可验证入口。      */
    @PostMapping("/order/{id}/pay")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.ok();
    }

    @PostMapping("/aftersale/apply")
    public Result<OmsAftersale> apply(@Valid @RequestBody ApplyDTO dto) {
        return Result.ok(aftersaleService.apply(dto.getOrderId(), dto.getOrderItemId(),
                dto.getType(), dto.getReason(), dto.getAmount()));
    }

    @Data
    public static class ApplyDTO {
        @NotNull(message = "订单号不能为空")
        private Long orderId;
        private Long orderItemId;
        @NotNull(message = "售后类型不能为空")
        @Min(value = 1, message = "售后类型不合法")
        @Max(value = 4, message = "售后类型不合法")
        private Integer type;
        @Size(max = 500, message = "原因过长")
        private String reason;
        @DecimalMin(value = "0.00", message = "退款金额不能为负")
        private BigDecimal amount;
    }
}
