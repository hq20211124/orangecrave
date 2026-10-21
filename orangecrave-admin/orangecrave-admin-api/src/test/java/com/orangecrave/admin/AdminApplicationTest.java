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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 冒烟测试：起完整 Spring 上下文（H2 + 种子），验证 健康检查 → 登录 → 带 token 取用户信息 → 菜单。  */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminApplicationTest {

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void healthLoginAndInfo() throws Exception {
        ResponseEntity<String> health = rest.getForEntity("/admin-api/health", String.class);
        assertThat(health.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(objectMapper.readTree(health.getBody()).path("code").asInt()).isZero();

        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);
        String loginBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        ResponseEntity<String> login = rest.postForEntity("/admin-api/auth/login",
                new HttpEntity<>(loginBody, json), String.class);
        JsonNode loginJson = objectMapper.readTree(login.getBody());
        assertThat(loginJson.path("code").asInt()).isZero();
        String token = loginJson.path("data").path("token").asText();
        assertThat(token).isNotBlank();

        HttpHeaders auth = new HttpHeaders();
        auth.set("Authorization", "Bearer " + token);
        ResponseEntity<String> info = rest.exchange("/admin-api/auth/info", HttpMethod.GET,
                new HttpEntity<>(auth), String.class);
        JsonNode infoJson = objectMapper.readTree(info.getBody());
        assertThat(infoJson.path("code").asInt()).isZero();
        assertThat(infoJson.path("data").path("username").asText()).isEqualTo("admin");
        assertThat(infoJson.path("data").path("perms").isArray()).isTrue();

        ResponseEntity<String> menus = rest.exchange("/admin-api/auth/menus", HttpMethod.GET,
                new HttpEntity<>(auth), String.class);
        assertThat(objectMapper.readTree(menus.getBody()).path("code").asInt()).isZero();

        ResponseEntity<String> unauth = rest.getForEntity("/admin-api/system/user/page", String.class);
        assertThat(objectMapper.readTree(unauth.getBody()).path("code").asInt()).isEqualTo(401);
    }
}
