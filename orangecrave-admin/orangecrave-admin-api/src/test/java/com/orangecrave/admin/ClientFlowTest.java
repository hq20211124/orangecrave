package com.orangecrave.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
     * 端到端主链路测试（验收 Q5）：
 * 渠道上报 → 顾客端 wx-login → 商品列表 → 计价预览 → 下单 → 我的订单 ← 管理员支付 ← 状态时间线。  */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ClientFlowTest {

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper json = new ObjectMapper();

    private HttpHeaders jsonHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        // 写操作必须带去重键（02 篇 §1.5）；每个请求独立一次，本测试不验证重放
        headers.set("Idempotency-Key", java.util.UUID.randomUUID().toString());
        return headers;
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        return json.readTree(response.getBody());
    }

    @Test
    void endToEndMainChain() throws Exception {
        // 1) 渠道上报（白名单，无需登录）
        ResponseEntity<String> visit = rest.postForEntity("/client-api/channel/visit",
                new HttpEntity<>("{\"channelCode\":\"xhs\"}", jsonHeaders(null)), String.class);
        assertThat(body(visit).path("code").asInt()).isZero();
        long channelId = body(visit).path("data").path("channelId").asLong();
        assertThat(channelId).isEqualTo(1L);

        // 2) 顾客端登录（桩）
        ResponseEntity<String> login = rest.postForEntity("/client-api/auth/wx-login",
                new HttpEntity<>("{\"code\":\"demo1\",\"channelId\":1}", jsonHeaders(null)), String.class);
        JsonNode loginJson = body(login);
        assertThat(loginJson.path("code").asInt()).isZero();
        String clientToken = loginJson.path("data").path("token").asText();
        assertThat(clientToken).isNotBlank();

        // 未带 token 访问顾客端受保护接口 → 401
        ResponseEntity<String> unauth = rest.getForEntity("/client-api/oms/order/mine", String.class);
        assertThat(body(unauth).path("code").asInt()).isEqualTo(401);

        // 3) 商品列表
        ResponseEntity<String> spuPage = rest.exchange("/client-api/pms/spu/page", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders(clientToken)), String.class);
        assertThat(body(spuPage).path("data").path("total").asLong()).isGreaterThan(0);

        // 4) 计价预览（0.00 × 10 = 20.00；未满 28 → 运费 4.00 → 应付 24.00）
        ResponseEntity<String> preview = rest.postForEntity("/client-api/oms/order/preview",
                new HttpEntity<>("{\"skuId\":1,\"qty\":10}", jsonHeaders(clientToken)), String.class);
        JsonNode priced = body(preview).path("data");
        assertThat(priced.path("itemAmount").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(priced.path("freight").decimalValue()).isEqualByComparingTo("4.00");
        assertThat(priced.path("payAmount").decimalValue()).isEqualByComparingTo("24.00");

        // 5) 下单
        String submitBody = "{\"skuId\":1,\"qty\":10,\"channelId\":1,"
                + "\"receiverSnapshot\":\"{\\\"name\\\":\\\"演示\\\"}\","
                + "\"promiseVersion\":\"v1.0\","
                + "\"images\":[{\"slotIndex\":0,\"imageUrl\":\"http://x/1.png\",\"copies\":10}]}";
        ResponseEntity<String> submit = rest.postForEntity("/client-api/oms/order/submit",
                new HttpEntity<>(submitBody, jsonHeaders(clientToken)), String.class);
        JsonNode order = body(submit).path("data");
        assertThat(body(submit).path("code").asInt()).isZero();
        long orderId = order.path("id").asLong();
        assertThat(order.path("status").asInt()).isZero();
        assertThat(order.path("payAmount").decimalValue()).isEqualByComparingTo("24.00");

        // 6) 我的订单
        ResponseEntity<String> mine = rest.exchange("/client-api/oms/order/mine", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders(clientToken)), String.class);
        assertThat(body(mine).path("data").path("total").asLong()).isEqualTo(1L);

        // 7) 顾客端支付（支付桩）
        ResponseEntity<String> pay = rest.postForEntity("/client-api/oms/order/" + orderId + "/pay",
                new HttpEntity<>(jsonHeaders(clientToken)), String.class);
        assertThat(body(pay).path("code").asInt()).isZero();

            // 8) 状态时间线：创建订单(0) + 支付成功(0→1)
        ResponseEntity<String> timeline = rest.exchange("/client-api/oms/order/" + orderId + "/timeline",
                HttpMethod.GET, new HttpEntity<>(jsonHeaders(clientToken)), String.class);
        JsonNode logs = body(timeline).path("data");
        assertThat(logs.isArray()).isTrue();
        assertThat(logs.size()).isEqualTo(2);
        assertThat(logs.get(1).path("toStatus").asInt()).isEqualTo(2);
    }
}
