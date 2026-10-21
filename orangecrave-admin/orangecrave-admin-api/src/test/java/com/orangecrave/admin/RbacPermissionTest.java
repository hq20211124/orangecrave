package com.orangecrave.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.system.entity.SysRole;
import com.orangecrave.admin.system.entity.SysUser;
import com.orangecrave.admin.system.entity.SysUserRole;
import com.orangecrave.admin.system.mapper.SysRoleMapper;
import com.orangecrave.admin.system.mapper.SysUserMapper;
import com.orangecrave.admin.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权限注解真实生效（02 篇 §1.4）：无 perm_key 的角色访问被 @SaCheckPermission 保护的接口要拿到 403，
 * 否则这 42 个注解只是装饰。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RbacPermissionTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper json = new ObjectMapper();

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private String login(String username, String password) throws Exception {
        ResponseEntity<String> response = rest.postForEntity("/admin-api/auth/login",
                new HttpEntity<>("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", jsonHeaders()),
                String.class);
        JsonNode node = json.readTree(response.getBody());
        assertThat(node.path("code").asInt()).isZero();
        return node.path("data").path("token").asText();
    }

    /** 客服角色（role_key=customer_service）只被授了 3 个菜单，不含营销。 */
    private String loginAsLimitedUser() {
        SysRole service = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, "customer_service"));
        assertThat(service).isNotNull();

        String username = "perm-" + UUID.randomUUID().toString().substring(0, 8);
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("test1234"));
        user.setNickname("权限测试用户");
        user.setStatus(1);
        userMapper.insert(user);

        SysUserRole relation = new SysUserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(service.getId());
        userRoleMapper.insert(relation);

        try {
            return login(username, "test1234");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    @Test
    void permissionAnnotationBlocksUnauthorizedRole() throws Exception {
        String token = loginAsLimitedUser();

        ResponseEntity<String> denied = rest.exchange("/admin-api/mkt/seckill/page", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);
        assertThat(json.readTree(denied.getBody()).path("code").asInt()).isEqualTo(403);

        // 同一接口换超级管理员即放行，说明差异来自 perm_key 而非接口本身
        String adminToken = login("admin", "admin123");
        ResponseEntity<String> allowed = rest.exchange("/admin-api/mkt/seckill/page", HttpMethod.GET,
                new HttpEntity<>(bearer(adminToken)), String.class);
        assertThat(json.readTree(allowed.getBody()).path("code").asInt()).isZero();
    }

    @Test
    void writeEndpointIsAlsoGuarded() throws Exception {
        String token = loginAsLimitedUser();
        HttpHeaders headers = bearer(token);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());

        ResponseEntity<String> denied = rest.postForEntity("/admin-api/mkt/seckill",
                new HttpEntity<>("{\"activityName\":\"越权测试\",\"ruleConfig\":\"{}\"}", headers), String.class);
        assertThat(json.readTree(denied.getBody()).path("code").asInt()).isEqualTo(403);
    }
}
