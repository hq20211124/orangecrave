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
 * 写操作幂等去重（02 篇 §1.5）：缺键拒绝、同键重放首次响应且不重复执行业务、换键可重做。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IdempotencyFilterTest {

    private static final String SECKILL_URL = "/admin-api/mkt/seckill";
    private static final String SECKILL_BODY = "{\"activityName\":\"幂等测试活动\",\"ruleConfig\":\"{}\"}";

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper json = new ObjectMapper();

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            h.set("Authorization", "Bearer " + token);
        }
        return h;
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        return json.readTree(response.getBody());
    }

    private String adminToken() throws Exception {
        ResponseEntity<String> login = rest.postForEntity("/admin-api/auth/login",
                new HttpEntity<>("{\"username\":\"admin\",\"password\":\"admin123\"}", headers(null)), String.class);
        String token = body(login).path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private long seckillTotal(String token) throws Exception {
        ResponseEntity<String> page = rest.exchange(SECKILL_URL + "/page", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        return body(page).path("data").path("total").asLong();
    }

    private ResponseEntity<String> create(String token, String idemKey) {
        HttpHeaders h = headers(token);
        if (idemKey != null) {
            h.set("Idempotency-Key", idemKey);
        }
        return rest.postForEntity(SECKILL_URL, new HttpEntity<>(SECKILL_BODY, h), String.class);
    }

    @Test
    void writeWithoutKeyIsRejected() throws Exception {
        String token = adminToken();
        long before = seckillTotal(token);

        assertThat(body(create(token, null)).path("code").asInt()).isEqualTo(1001);
        assertThat(seckillTotal(token)).isEqualTo(before);
    }

    @Test
    void sameKeyReplaysFirstResponseWithoutSecondInsert() throws Exception {
        String token = adminToken();
        String key = java.util.UUID.randomUUID().toString();

        long before = seckillTotal(token);
        ResponseEntity<String> first = create(token, key);
        assertThat(body(first).path("code").asInt()).isZero();
        assertThat(seckillTotal(token)).isEqualTo(before + 1);

        ResponseEntity<String> replay = create(token, key);
        assertThat(body(replay).path("code").asInt()).isZero();
        assertThat(replay.getBody()).isEqualTo(first.getBody());
        assertThat(seckillTotal(token)).isEqualTo(before + 1);

        // 换键即可重做：去重的是「同键 + 同接口」，不是接口本身
        assertThat(body(create(token, java.util.UUID.randomUUID().toString())).path("code").asInt()).isZero();
        assertThat(seckillTotal(token)).isEqualTo(before + 2);
    }

    @Test
    void failedBusinessCallKeepsKeyRetryable() throws Exception {
        String token = adminToken();
        String key = java.util.UUID.randomUUID().toString();
        long before = seckillTotal(token);

        // 无令牌先占键：鉴权失败后记录置为可重试，同键仍能再执行一次
        assertThat(body(create(null, key)).path("code").asInt()).isNotZero();
        assertThat(seckillTotal(token)).isEqualTo(before);

        assertThat(body(create(token, key)).path("code").asInt()).isZero();
        assertThat(seckillTotal(token)).isEqualTo(before + 1);
    }

    @Test
    void readOnlyEndpointsAreExempt() throws Exception {
        String token = adminToken();
        // GET 与 POST 承载的只读接口（计价预览）都不要求幂等键
        assertThat(body(rest.exchange(SECKILL_URL + "/page", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class)).path("code").asInt()).isZero();

        ResponseEntity<String> wxLogin = rest.postForEntity("/client-api/auth/wx-login",
                new HttpEntity<>("{\"code\":\"idem-preview\",\"channelId\":1}", headers(null)), String.class);
        String clientToken = body(wxLogin).path("data").path("token").asText();
        ResponseEntity<String> preview = rest.postForEntity("/client-api/oms/order/preview",
                new HttpEntity<>("{\"skuId\":1,\"qty\":10}", headers(clientToken)), String.class);
        assertThat(body(preview).path("code").asInt()).isZero();
    }
}
