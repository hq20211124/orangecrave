package com.orangecrave.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.limit.RateLimitFilter;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 秒杀削峰端到端测试（03 篇 §2.6）：受理 → 轮询 → 拿到结果。
 *
 * <p>测试 profile 不开 Redis，投递走同步直投实现——请求返回时消费者已就地跑完，
 * 因此轮询第一次就能拿到终态。这样「削峰开关关掉时功能不变、只是没了异步」这条契约
 * 也被验证到：不开 Redis 的开发与测试环境，链路必须照样通。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SeckillOrderFlowTest {

    @Autowired
    private TestRestTemplate rest;

    /** 装配断言：该 Bean 缺失（限流未生效）会让本测试直接启动失败。 */
    @Autowired
    private RateLimitFilter rateLimitFilter;

    private final ObjectMapper json = new ObjectMapper();

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        return headers;
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        return json.readTree(response.getBody());
    }

    /** 独立登录用 code：不复用 ClientFlowTest 的演示用户，避免它的「我的订单数」断言被本测试的订单干扰。 */
    private String login() throws Exception {
        ResponseEntity<String> login = rest.postForEntity("/client-api/auth/wx-login",
                new HttpEntity<>("{\"code\":\"seckill1\",\"channelId\":1}", headers(null)), String.class);
        JsonNode node = body(login);
        assertThat(node.path("code").asInt()).isZero();
        return node.path("data").path("token").asText();
    }

    private String submitBody(long skuId) {
        return "{\"skuId\":" + skuId + ",\"qty\":10,\"channelId\":1,\"activityId\":2,"
                + "\"receiverSnapshot\":\"{\\\"name\\\":\\\"演示\\\"}\","
                + "\"promiseVersion\":\"v1.0\","
                + "\"images\":[{\"slotIndex\":0,\"imageUrl\":\"http://x/1.png\",\"copies\":10}]}";
    }

    @Test
    void seckillSubmitIsAcceptedThenPolledToSuccess() throws Exception {
        String token = login();

        // 1) 受理：立刻拿到 requestId，此时订单还没落库
        ResponseEntity<String> accepted = rest.postForEntity("/client-api/oms/order/seckill",
                new HttpEntity<>(submitBody(1L), headers(token)), String.class);
        JsonNode acceptedJson = body(accepted);
        assertThat(acceptedJson.path("code").asInt()).isZero();
        String requestId = acceptedJson.path("data").path("requestId").asText();
        assertThat(requestId).hasSize(32);
        assertThat(acceptedJson.path("data").path("status").asText()).isEqualTo("QUEUED");

        // 2) 轮询结果：同步直投下消费者已就地完成
        ResponseEntity<String> result = rest.exchange("/client-api/oms/order/seckill/result/" + requestId,
                HttpMethod.GET, new HttpEntity<>(headers(token)), String.class);
        JsonNode resultJson = body(result).path("data");
        assertThat(resultJson.path("status").asText()).isEqualTo("SUCCESS");
        long orderId = resultJson.path("orderId").asLong();
        assertThat(orderId).isGreaterThan(0);

        // 3) 订单确实落库，且出现在该用户的订单列表里
        ResponseEntity<String> mine = rest.exchange("/client-api/oms/order/mine", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        JsonNode orders = body(mine).path("data").path("list");
        assertThat(orders.findValues("id")).anySatisfy(id -> assertThat(id.asLong()).isEqualTo(orderId));
    }

    /** 下单被业务拒绝（如 SKU 不存在）：受理仍然成功，失败原因通过轮询结果回给客户端。 */
    @Test
    void rejectedSubmitSurfacesReasonThroughPolling() throws Exception {
        String token = login();

        ResponseEntity<String> accepted = rest.postForEntity("/client-api/oms/order/seckill",
                new HttpEntity<>(submitBody(999_999L), headers(token)), String.class);
        assertThat(body(accepted).path("code").asInt()).isZero();
        String requestId = body(accepted).path("data").path("requestId").asText();

        JsonNode result = body(rest.exchange("/client-api/oms/order/seckill/result/" + requestId,
                HttpMethod.GET, new HttpEntity<>(headers(token)), String.class)).path("data");
        assertThat(result.path("status").asText()).isEqualTo("FAILED");
        assertThat(result.path("message").asText()).isNotBlank();
    }

    /** 未知 requestId（或结果已过期）返回 data=null，客户端据此停止轮询。 */
    @Test
    void unknownRequestIdReturnsNullData() throws Exception {
        String token = login();

        ResponseEntity<String> result = rest.exchange("/client-api/oms/order/seckill/result/deadbeef",
                HttpMethod.GET, new HttpEntity<>(headers(token)), String.class);

        assertThat(body(result).path("code").asInt()).isZero();
        assertThat(body(result).path("data").isNull()).isTrue();
    }
}
