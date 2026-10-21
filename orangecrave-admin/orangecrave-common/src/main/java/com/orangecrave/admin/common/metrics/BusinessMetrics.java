package com.orangecrave.admin.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * 业务指标统一入口。所有自定义指标从这里注册，命名前缀 orangecrave_。
 * 使用方式：注入 BusinessMetrics，在业务代码中调用对应方法。
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** 订单提交计数：tag=status(success/fail) */
    public Counter orderSubmit(String status) {
        return Counter.builder("orangecrave_order_submit_total")
                .tag("status", status)
                .register(registry);
    }

    /** 秒杀预扣计数：tag=result(pass/reject) */
    public Counter seckillReserve(String result) {
        return Counter.builder("orangecrave_seckill_reserve_total")
                .tag("result", result)
                .register(registry);
    }

    /** 限流触发计数：tag=path */
    public Counter rateLimitHit(String path) {
        return Counter.builder("orangecrave_rate_limit_hit_total")
                .tag("path", path)
                .register(registry);
    }

    /** MQ 消息投递延迟 */
    public Timer mqPublishLatency() {
        return Timer.builder("orangecrave_mq_publish_seconds")
                .register(registry);
    }

    /** 支付结果计数：tag=result(success/fail) */
    public Counter paymentResult(String result) {
        return Counter.builder("orangecrave_payment_total")
                .tag("result", result)
                .register(registry);
    }
}
