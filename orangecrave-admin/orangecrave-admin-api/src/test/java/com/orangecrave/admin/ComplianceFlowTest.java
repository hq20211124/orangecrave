package com.orangecrave.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.audit.entity.AudSensitiveWord;
import com.orangecrave.admin.audit.mapper.AudSensitiveWordMapper;
import com.orangecrave.admin.user.entity.UmsIdentityCert;
import com.orangecrave.admin.user.mapper.UmsIdentityCertMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文本审核与版权承诺链路（05 篇 §2.3 §2.4、07 篇 §4.2、03 篇 §2.1 状态 13）。
 * 词典里的词是明显虚构的测试词，不用真实违规内容。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ComplianceFlowTest {

    private static final String REJECT_WORD = "测试违禁甲";
    private static final String REPLACE_WORD = "测试替换乙";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private AudSensitiveWordMapper wordMapper;

    @Autowired
    private UmsIdentityCertMapper certMapper;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void seedWords() {
        word("违禁类", REJECT_WORD, "拒绝");
        word("轻度类", REPLACE_WORD, "替换");
    }

    private void word(String category, String w, String action) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM aud_sensitive_word WHERE word = ?", Integer.class, w) > 0) {
            return;
        }
        AudSensitiveWord word = new AudSensitiveWord();
        word.setWord(w);
        word.setCategory(category);
        word.setAction(action);
        word.setStatus(1);
        wordMapper.insert(word);
    }

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

    private String adminToken() throws Exception {
        ResponseEntity<String> login = rest.postForEntity("/admin-api/auth/login",
                new HttpEntity<>("{\"username\":\"admin\",\"password\":\"admin123\"}", headers(null)), String.class);
        return body(login).path("data").path("token").asText();
    }

    /** 每个用例一个新顾客，避免下单去重与状态互相影响。 */
    private JsonNode clientLogin() throws Exception {
        ResponseEntity<String> login = rest.postForEntity("/client-api/auth/wx-login",
                new HttpEntity<>("{\"code\":\"" + UUID.randomUUID().toString().substring(0, 8) + "\",\"channelId\":1}",
                        headers(null)), String.class);
        return body(login).path("data");
    }

    private String submitBody(String promiseVersion) {
        String images = "[{\"slotIndex\":0,\"imageUrl\":\"http://x/1.png\",\"copies\":10}]";
        return "{\"skuId\":1,\"qty\":10,\"channelId\":1,"
                + "\"receiverSnapshot\":\"{\\\"name\\\":\\\"演示收货人\\\",\\\"phone\\\":\\\"13800000000\\\"}\","
                + (promiseVersion == null ? "" : "\"promiseVersion\":\"" + promiseVersion + "\",")
                + "\"images\":" + images + "}";
    }

    private long submitOrder(JsonNode client, String promiseVersion) throws Exception {
        ResponseEntity<String> submit = rest.postForEntity("/client-api/oms/order/submit",
                new HttpEntity<>(submitBody(promiseVersion), headers(client.path("token").asText())), String.class);
        assertThat(body(submit).path("code").asInt()).isZero();
        return body(submit).path("data").path("id").asLong();
    }

    @Test
    void copyrightPromiseIsRequiredAndKeptAsEvidence() throws Exception {
        JsonNode client = clientLogin();

        // 带图稿却没有承诺版本 → 拒单（07 篇 §4.2）
        ResponseEntity<String> refused = rest.postForEntity("/client-api/oms/order/submit",
                new HttpEntity<>(submitBody(null), headers(client.path("token").asText())), String.class);
        assertThat(body(refused).path("code").asInt()).isNotZero();
        assertThat(body(refused).path("msg").asText()).contains("版权承诺");

        long orderId = submitOrder(client, "v1.0");
        Integer promised = jdbc.queryForObject(
                "SELECT COUNT(*) FROM aud_copyright_promise WHERE biz_type = 1 AND biz_id = ? AND promise_version = 'v1.0'",
                Integer.class, orderId);
        assertThat(promised).isEqualTo(1);
    }

    @Test
    void orderRemarkIsMaskedOrRefusedByTheWordList() throws Exception {
        String admin = adminToken();
        long orderId = submitOrder(clientLogin(), "v1.0");

        ResponseEntity<String> masked = rest.exchange("/admin-api/oms/order/" + orderId + "/remark", HttpMethod.PUT,
                new HttpEntity<>("{\"remark\":\"备注里有" + REPLACE_WORD + "\"}", headers(admin)), String.class);
        assertThat(body(masked).path("code").asInt()).isZero();
        ResponseEntity<String> detail = rest.exchange("/admin-api/oms/order/" + orderId,
                HttpMethod.GET, new HttpEntity<>(headers(admin)), String.class);
        String remark = body(detail).path("data").path("remark").asText();
        assertThat(remark).doesNotContain(REPLACE_WORD).contains("*");

        ResponseEntity<String> refused = rest.exchange("/admin-api/oms/order/" + orderId + "/remark", HttpMethod.PUT,
                new HttpEntity<>("{\"remark\":\"这里写了" + REJECT_WORD + "\"}", headers(admin)), String.class);
        assertThat(body(refused).path("code").asInt()).isNotZero();
        // 提示里带上违规词，操作者才知道要改什么
        assertThat(body(refused).path("msg").asText()).contains(REJECT_WORD);
    }

    @Test
    void ticketTextGoesThroughTheSameFilter() throws Exception {
        String admin = adminToken();
        long userId = clientLogin().path("userId").asLong();

        ResponseEntity<String> refused = rest.postForEntity("/admin-api/svc/ticket",
                new HttpEntity<>("{\"userId\":" + userId + ",\"type\":1,\"title\":\"标题含" + REJECT_WORD
                        + "\",\"content\":\"内容\"}", headers(admin)), String.class);
        assertThat(body(refused).path("code").asInt()).isNotZero();
        assertThat(body(refused).path("msg").asText()).contains(REJECT_WORD);

        ResponseEntity<String> created = rest.postForEntity("/admin-api/svc/ticket",
                new HttpEntity<>("{\"userId\":" + userId + ",\"type\":1,\"title\":\"标题含" + REPLACE_WORD
                        + "\",\"content\":\"正常内容\"}", headers(admin)), String.class);
        assertThat(body(created).path("code").asInt()).isZero();
        assertThat(body(created).path("data").path("title").asText()).doesNotContain(REPLACE_WORD).contains("*");
    }

    @Test
    void certRejectionTurnsPendingOrdersIntoIllegalCancellations() throws Exception {
        String admin = adminToken();
        JsonNode client = clientLogin();
        long userId = client.path("userId").asLong();
        long orderId = submitOrder(client, "v1.0");

        UmsIdentityCert cert = new UmsIdentityCert();
        cert.setUserId(userId);
        cert.setIdentityType(1);
        cert.setRealName("测试用户A");
        cert.setIdCardNo("110101199003071234");
        cert.setStatus(1);
        certMapper.insert(cert);

        ResponseEntity<String> reject = rest.postForEntity("/admin-api/ums/cert/" + cert.getId() + "/reject",
                new HttpEntity<>("{\"remark\":\"证件与本人不符\"}", headers(admin)), String.class);
        assertThat(body(reject).path("code").asInt()).isZero();

        // 03 篇 §2.1：认证被驳回的团长，其未进生产的订单走状态 13，不与超时取消（9）混用
        ResponseEntity<String> detail = rest.exchange("/admin-api/oms/order/" + orderId,
                HttpMethod.GET, new HttpEntity<>(headers(admin)), String.class);
        assertThat(body(detail).path("data").path("status").asInt()).isEqualTo(13);
    }
}
