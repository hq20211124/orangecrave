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
 * mass-assignment 护栏：写接口只收 DTO 白名单字段，审计三列（deleted / create_time / update_time）
 * 在反序列化层被忽略（见 {@code JacksonConfig} 与 02 篇 §1.4），服务端派生列不在入参里。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MassAssignmentTest {

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper json = new ObjectMapper();

    private HttpHeaders adminHeaders() throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Idempotency-Key", UUID.randomUUID().toString());
        ResponseEntity<String> login = rest.postForEntity("/admin-api/auth/login",
                new HttpEntity<>("{\"username\":\"admin\",\"password\":\"admin123\"}", h), String.class);
        h.set("Authorization", "Bearer " + json.readTree(login.getBody()).path("data").path("token").asText());
        return h;
    }

    @Test
    void auditColumnsCannotBeInjectedThroughRequestBody() throws Exception {
        HttpHeaders headers = adminHeaders();
        String name = "护栏测试系列" + UUID.randomUUID().toString().substring(0, 8);

        // 客户端在正常业务字段之外多塞 deleted / createTime
        ResponseEntity<String> create = rest.postForEntity("/admin-api/pms/series",
                new HttpEntity<>("{\"seriesName\":\"" + name + "\",\"ipName\":\"测试IP\",\"sort\":1,\"status\":1,"
                        + "\"deleted\":1,\"createTime\":\"2000-01-01T00:00:00\"}", headers), String.class);
        assertThat(json.readTree(create.getBody()).path("code").asInt()).isZero();

        ResponseEntity<String> page = rest.exchange("/admin-api/pms/series/page?keyword=" + name,
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        JsonNode rows = json.readTree(page.getBody()).path("data").path("list");
        assertThat(rows.size()).isEqualTo(1);
        JsonNode created = rows.get(0);
        assertThat(created.path("seriesName").asText()).isEqualTo(name);
        // 逻辑删除位只认框架（新建恒为 0），创建时间由填充器写当前时间：
        // 客户端伪造的 2000-01-01 若被反序列化收下，这里就会露馅。
        assertThat(created.path("deleted").asInt()).isZero();
        assertThat(created.path("createTime").asText()).doesNotStartWith("2000");
    }

    /** 服务端派生列（已发放张数）只能由发放动作累加，建券入参里没有这一项。 */
    @Test
    void serverOwnedCountersAreNotSettableThroughTheSaveDto() throws Exception {
        HttpHeaders headers = adminHeaders();
        String name = "护栏测试券" + UUID.randomUUID().toString().substring(0, 8);

        ResponseEntity<String> create = rest.postForEntity("/admin-api/mkt/coupon",
                new HttpEntity<>("{\"couponName\":\"" + name + "\",\"couponType\":1,\"threshold\":20.00,"
                        + "\"amount\":5.00,\"validDays\":30,\"totalQty\":100,\"issuedQty\":9999}", headers), String.class);
        assertThat(json.readTree(create.getBody()).path("code").asInt()).isZero();

        ResponseEntity<String> page = rest.exchange("/admin-api/mkt/coupon/page?pageSize=1",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        JsonNode latest = json.readTree(page.getBody()).path("data").path("list").get(0);
        assertThat(latest.path("couponName").asText()).isEqualTo(name);
        assertThat(latest.path("totalQty").asInt()).isEqualTo(100);
        assertThat(latest.path("issuedQty").asInt()).isZero();
    }
}
