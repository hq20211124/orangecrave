package com.orangecrave.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.marketing.entity.MktCouponUser;
import com.orangecrave.admin.marketing.mapper.MktCouponUserMapper;
import com.orangecrave.admin.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 计价与优惠顺序的端到端测试（04 篇 §4）：
 * 活动价覆盖原价 → 优惠券抵扣 → 券前金额判包邮 → 运费；券的锁定/核销/返还（§3.4）。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PricingDiscountFlowTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private MktCouponUserMapper couponUserMapper;
    @Autowired
    private OrderService orderService;

    private final ObjectMapper json = new ObjectMapper();

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            h.set("Authorization", "Bearer " + token);
        }
        h.set("Idempotency-Key", UUID.randomUUID().toString());
        return h;
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        return json.readTree(response.getBody());
    }

    private JsonNode clientLogin(String code) throws Exception {
        ResponseEntity<String> login = rest.postForEntity("/client-api/auth/wx-login",
                new HttpEntity<>("{\"code\":\"" + code + "\",\"channelId\":1}", headers(null)), String.class);
        JsonNode data = body(login).path("data");
        assertThat(data.path("token").asText()).isNotBlank();
        return data;
    }

    private JsonNode preview(String token, String payload) throws Exception {
        return body(rest.postForEntity("/client-api/oms/order/preview",
                new HttpEntity<>(payload, headers(token)), String.class));
    }

    private long grantCoupon(Long userId, long couponTemplateId) {
        MktCouponUser couponUser = new MktCouponUser();
        couponUser.setCouponId(couponTemplateId);
        couponUser.setUserId(userId);
        couponUser.setCode("T" + UUID.randomUUID().toString().substring(0, 10));
        couponUser.setStatus(1);
        couponUser.setExpireTime(LocalDateTime.now().plusDays(30));
        couponUser.setSource(1);
        couponUserMapper.insert(couponUser);
        return couponUser.getId();
    }

    @Test
    void activityPriceCouponAndFreightFollowTheDocumentedOrder() throws Exception {
        JsonNode session = clientLogin("pricing-1");
        String token = session.path("token").asText();
        long userId = session.path("userId").asLong();

        // 原价：2.00 × 10 = 20.00，未满 28 → 运费 4.00
        JsonNode plain = preview(token, "{\"skuId\":1,\"qty\":10}").path("data");
        assertThat(plain.path("itemAmount").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(plain.path("discountAmount").decimalValue()).isEqualByComparingTo("0");
        assertThat(plain.path("freight").decimalValue()).isEqualByComparingTo("4.00");
        assertThat(plain.path("payAmount").decimalValue()).isEqualByComparingTo("24.00");

        // 拼团价 1.50 覆盖按张原价；20 张 = 30.00 已达包邮阈值，只免基础邮费
        JsonNode group = preview(token, "{\"skuId\":1,\"qty\":10,\"activityId\":1}").path("data");
        assertThat(group.path("basePrice").decimalValue()).isEqualByComparingTo("15.00");
        assertThat(group.path("payAmount").decimalValue()).isEqualByComparingTo("19.00");
        JsonNode group20 = preview(token, "{\"skuId\":1,\"qty\":20,\"activityId\":1}").path("data");
        assertThat(group20.path("itemAmount").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(group20.path("freight").decimalValue()).isEqualByComparingTo("0.00");

        // 秒杀价 1.00；停用活动 id=3 不生效，按原价
        assertThat(preview(token, "{\"skuId\":1,\"qty\":10,\"activityId\":2}").path("data")
                .path("itemAmount").decimalValue()).isEqualByComparingTo("10.00");
        assertThat(preview(token, "{\"skuId\":1,\"qty\":10,\"activityId\":3}").path("data")
                .path("itemAmount").decimalValue()).isEqualByComparingTo("20.00");

        // 满 20 减 5：包邮判定仍看券前 20.00（未满 28），所以运费照收
        long couponId = grantCoupon(userId, 1L);
        JsonNode withCoupon = preview(token, "{\"skuId\":1,\"qty\":10,\"couponUserId\":" + couponId + "}").path("data");
        assertThat(withCoupon.path("discountAmount").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(withCoupon.path("freight").decimalValue()).isEqualByComparingTo("4.00");
        assertThat(withCoupon.path("payAmount").decimalValue()).isEqualByComparingTo("19.00");

        // 04 篇 §3.2.2：活动价与优惠券互斥
        JsonNode both = preview(token, "{\"skuId\":1,\"qty\":10,\"activityId\":1,\"couponUserId\":" + couponId + "}");
        assertThat(both.path("code").asInt()).isNotZero();
        assertThat(both.path("msg").asText()).contains("不可叠加");
    }

    @Test
    void couponIsLockedOnSubmitConsumedOnPayAndReleasedOnCancel() throws Exception {
        JsonNode session = clientLogin("pricing-2");
        String token = session.path("token").asText();
        long userId = session.path("userId").asLong();

        long couponA = grantCoupon(userId, 1L);
        long couponB = grantCoupon(userId, 1L);

        // 下单即锁券：订单落库优惠/实付，券绑定订单但仍是未使用
        ResponseEntity<String> submitA = rest.postForEntity("/client-api/oms/order/submit",
                new HttpEntity<>("{\"skuId\":1,\"qty\":10,\"couponUserId\":" + couponA
                        + ",\"receiverSnapshot\":\"{\\\"name\\\":\\\"演示\\\"}\"}", headers(token)), String.class);
        JsonNode orderA = body(submitA).path("data");
        assertThat(body(submitA).path("code").asInt()).isZero();
        assertThat(orderA.path("totalAmount").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(orderA.path("discountAmount").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(orderA.path("payAmount").decimalValue()).isEqualByComparingTo("19.00");

        MktCouponUser locked = couponUserMapper.selectById(couponA);
        assertThat(locked.getStatus()).isEqualTo(1);
        assertThat(locked.getOrderId()).isEqualTo(orderA.path("id").asLong());

        // 已被占用的券不能再下第二单
        ResponseEntity<String> reuse = rest.postForEntity("/client-api/oms/order/submit",
                new HttpEntity<>("{\"skuId\":1,\"qty\":10,\"couponUserId\":" + couponA
                        + ",\"receiverSnapshot\":\"{\\\"name\\\":\\\"演示\\\"}\"}", headers(token)), String.class);
        assertThat(body(reuse).path("code").asInt()).isNotZero();

        // 支付核销
        ResponseEntity<String> pay = rest.postForEntity("/client-api/oms/order/"
                + orderA.path("id").asLong() + "/pay", new HttpEntity<>(headers(token)), String.class);
        assertThat(body(pay).path("code").asInt()).isZero();
        MktCouponUser used = couponUserMapper.selectById(couponA);
        assertThat(used.getStatus()).isEqualTo(2);
        assertThat(used.getUseTime()).isNotNull();

        // 未付款订单取消 → 返还券
        ResponseEntity<String> submitB = rest.postForEntity("/client-api/oms/order/submit",
                new HttpEntity<>("{\"skuId\":1,\"qty\":10,\"couponUserId\":" + couponB
                        + ",\"receiverSnapshot\":\"{\\\"name\\\":\\\"演示\\\"}\"}", headers(token)), String.class);
        long orderBId = body(submitB).path("data").path("id").asLong();
        assertThat(body(submitB).path("data").path("payAmount").decimalValue()).isEqualByComparingTo("19.00");
        assertThat(orderService.cancel(orderBId, "系统", null)).isTrue();

        MktCouponUser released = couponUserMapper.selectById(couponB);
        assertThat(released.getStatus()).isEqualTo(1);
        assertThat(released.getOrderId()).isNull();
        assertThat(released.getUseTime()).isNull();
    }
}
