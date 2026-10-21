package com.orangecrave.admin.order.config;

import com.orangecrave.admin.finance.service.FinRefundService;
import com.orangecrave.admin.order.adapter.ManualProductionAdapter;
import com.orangecrave.admin.order.adapter.PaymentAdapter;
import com.orangecrave.admin.order.adapter.ProductionAdapter;
import com.orangecrave.admin.order.adapter.RefundAdapter;
import com.orangecrave.admin.order.adapter.StubPaymentAdapter;
import com.orangecrave.admin.order.adapter.StubRefundAdapter;
import com.orangecrave.admin.order.adapter.StubWaybillAdapter;
import com.orangecrave.admin.order.adapter.WaybillAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 适配器装配（见 03 篇 §10）。v1 全部走桩；迁移时只换实现类，业务代码不动。  */
@Configuration
@RequiredArgsConstructor
public class AdapterConfig {

    private final FinRefundService finRefundService;

    @Bean
    public PaymentAdapter paymentAdapter() {
        return new StubPaymentAdapter();
    }

    @Bean
    public RefundAdapter refundAdapter() {
        return new StubRefundAdapter(finRefundService);
    }

    @Bean
    public WaybillAdapter waybillAdapter() {
        return new StubWaybillAdapter();
    }

    @Bean
    public ProductionAdapter productionAdapter() {
        return new ManualProductionAdapter();
    }
}
