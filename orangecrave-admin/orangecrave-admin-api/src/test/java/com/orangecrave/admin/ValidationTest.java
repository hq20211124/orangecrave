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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 入参校验测试（02 篇 §1.4）：@Valid 必须真的拦住非法请求，而不是只挂在签名上。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ValidationTest {

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper json = new ObjectMapper();

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Idempotency-Key", UUID.randomUUID().toString());
        if (token != null) {
            h.set("Authorization", "Bearer " + token);
        }
        return h;
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        return json.readTree(response.getBody());
    }

    private String clientToken() throws Exception {
        ResponseEntity<String> login = rest.postForEntity("/client-api/auth/wx-login",
                new HttpEntity<>("{\"code\":\"valid-user\",\"channelId\":1}", headers(null)), String.class);
        return body(login).path("data").path("token").asText();
    }

    private String adminToken() throws Exception {
        ResponseEntity<String> login = rest.postForEntity("/admin-api/auth/login",
                new HttpEntity<>("{\"username\":\"admin\",\"password\":\"admin123\"}", headers(null)), String.class);
        return body(login).path("data").path("token").asText();
    }

    @Test
    void missingRequiredFieldsAreRejectedWithBusinessCode() throws Exception {
        String token = clientToken();

        // 缺 skuId
        ResponseEntity<String> preview = rest.postForEntity("/client-api/oms/order/preview",
                new HttpEntity<>("{\"qty\":10}", headers(token)), String.class);
        assertThat(body(preview).path("code").asInt()).isEqualTo(1001);
        assertThat(body(preview).path("msg").asText()).contains("规格");

        // 数量非法
        ResponseEntity<String> badQty = rest.postForEntity("/client-api/oms/order/preview",
                new HttpEntity<>("{\"skuId\":1,\"qty\":0}", headers(token)), String.class);
        assertThat(body(badQty).path("code").asInt()).isEqualTo(1001);

        // 图槽位缺图片：嵌套对象要级联校验
        ResponseEntity<String> submit = rest.postForEntity("/client-api/oms/order/submit",
                new HttpEntity<>("{\"skuId\":1,\"qty\":10,\"images\":[{\"slotIndex\":0,\"copies\":10}]}",
                        headers(token)), String.class);
        assertThat(body(submit).path("code").asInt()).isEqualTo(1001);
        assertThat(body(submit).path("msg").asText()).contains("图稿地址");
    }

    @Test
    void adminWriteEndpointsValidateTheirBodies() throws Exception {
        String token = adminToken();

        ResponseEntity<String> status = rest.exchange("/admin-api/oms/order/1/status", HttpMethod.PUT,
                new HttpEntity<>("{\"remark\":\"忘记传状态\"}", headers(token)), String.class);
        assertThat(body(status).path("code").asInt()).isEqualTo(1001);
        assertThat(body(status).path("msg").asText()).contains("状态");

        ResponseEntity<String> export = rest.postForEntity("/admin-api/oms/order/batch-export",
                new HttpEntity<>("{\"ids\":[]}", headers(token)), String.class);
        assertThat(body(export).path("code").asInt()).isEqualTo(1001);
    }

    @Test
    void negativeMoneyIsRejectedButAbsenceMeansDefaultValue() throws Exception {
        String token = adminToken();

        // 改价允许只传部分字段（未传即保持原值），但负数必须拦下
        ResponseEntity<String> negative = rest.exchange("/admin-api/oms/order/1/price", HttpMethod.PUT,
                new HttpEntity<>("{\"payAmount\":-1.00}", headers(token)), String.class);
        assertThat(body(negative).path("code").asInt()).isEqualTo(1001);
        assertThat(body(negative).path("msg").asText()).contains("不能为负");
    }
}
