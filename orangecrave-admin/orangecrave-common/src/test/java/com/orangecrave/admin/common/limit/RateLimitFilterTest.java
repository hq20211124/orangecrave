package com.orangecrave.admin.common.limit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 限流过滤器测试。用 mock 请求/响应直接驱动过滤器，不起 Web 容器、不依赖 Redis，
 * 逻辑（前缀匹配、身份分片、拒绝信封）全部确定可断言。
 */
class RateLimitFilterTest {

    private static final String SECKILL_PATH = "/client-api/oms/order/seckill";

    private final ObjectMapper json = new ObjectMapper();

    /** 放行次数：过滤器调了 chain 才算放行，被拒绝的请求不会走到这里。 */
    private int passed;

    private final FilterChain chain = (request, response) -> passed++;

    private RateLimitProperties properties(String prefix, int permitsPerSecond, int burst) {
        RateLimitProperties properties = new RateLimitProperties();
        RateLimitProperties.Rule rule = new RateLimitProperties.Rule();
        rule.setPermitsPerSecond(permitsPerSecond);
        rule.setBurst(burst);
        properties.getRules().put(prefix, rule);
        return properties;
    }

    private MockHttpServletRequest request(String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    /** 未声明的路径一律放行：限流只作用于显式配置的入口，不对全站一刀切。 */
    @Test
    void unmatchedPathIsNotLimited() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new LocalRateLimiterFactory(),
                properties(SECKILL_PATH, 1, 1), json);

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request("/client-api/pms/spu/page", "10.0.0.1"),
                    new MockHttpServletResponse(), chain);
        }
        assertThat(passed).isEqualTo(5);
    }

    /** 命中前缀且令牌耗尽：不放行，并按全站契约回信封（HTTP 200 + code 429）。 */
    @Test
    void exhaustedBucketIsRejectedWithEnvelope() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new LocalRateLimiterFactory(),
                properties(SECKILL_PATH, 1, 1), json);

        filter.doFilterInternal(request(SECKILL_PATH, "10.0.0.1"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse rejected = new MockHttpServletResponse();
        filter.doFilterInternal(request(SECKILL_PATH, "10.0.0.1"), rejected, chain);

        assertThat(passed).isEqualTo(1);
        assertThat(rejected.getStatus()).isEqualTo(200);
        JsonNode body = json.readTree(rejected.getContentAsString());
        assertThat(body.path("code").asInt()).isEqualTo(429);
        assertThat(body.path("msg").asText()).isNotBlank();
    }

    /** 未登录时按来源 IP 分片：一个 IP 被限住不该牵连同一台机器上的其他访问者。 */
    @Test
    void unauthenticatedRequestsAreBucketizedByIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new LocalRateLimiterFactory(),
                properties(SECKILL_PATH, 1, 1), json);

        filter.doFilterInternal(request(SECKILL_PATH, "10.0.0.1"), new MockHttpServletResponse(), chain);
        filter.doFilterInternal(request(SECKILL_PATH, "10.0.0.1"), new MockHttpServletResponse(), chain);
        filter.doFilterInternal(request(SECKILL_PATH, "10.0.0.2"), new MockHttpServletResponse(), chain);

        assertThat(passed).isEqualTo(2);
    }

    /** 相邻路径不受牵连：规则按精确路径命中，秒杀结果轮询不会被提交入口的令牌桶限住。 */
    @Test
    void adjacentPathIsNotLimitedByExactRule() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(new LocalRateLimiterFactory(),
                properties(SECKILL_PATH, 1, 1), json);
        String resultPath = SECKILL_PATH + "/result/abc";

        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(request(resultPath, "10.0.0.1"), new MockHttpServletResponse(), chain);
        }

        assertThat(passed).isEqualTo(5);
    }

    /** 子树规则覆盖其下所有路径；与更精确的规则同时命中时取更长者。 */
    @Test
    void subtreeRuleLosesToMoreSpecificRule() throws Exception {
        RateLimitProperties properties = properties("/client-api/**", 100, 100);
        RateLimitProperties.Rule seckill = new RateLimitProperties.Rule();
        seckill.setPermitsPerSecond(1);
        seckill.setBurst(1);
        properties.getRules().put(SECKILL_PATH, seckill);

        RateLimitFilter filter = new RateLimitFilter(new LocalRateLimiterFactory(), properties, json);
        filter.doFilterInternal(request(SECKILL_PATH, "10.0.0.1"), new MockHttpServletResponse(), chain);
        filter.doFilterInternal(request(SECKILL_PATH, "10.0.0.1"), new MockHttpServletResponse(), chain);
        // 子树规则内的其它路径仍按宽规则放行
        filter.doFilterInternal(request("/client-api/pms/spu/page", "10.0.0.1"), new MockHttpServletResponse(), chain);

        // 若误用了宽容的子树规则，第一段会是 2 而不是 1
        assertThat(passed).isEqualTo(2);
    }

    /** 总开关与空规则都直接放行——线上排查时第一件事就是把限流关掉。 */
    @Test
    void disabledOrEmptyRulesSkipFiltering() {
        RateLimitProperties disabled = properties(SECKILL_PATH, 1, 1);
        disabled.setEnabled(false);
        assertThat(new RateLimitFilter(new LocalRateLimiterFactory(), disabled, json)
                .shouldNotFilter(request(SECKILL_PATH, "10.0.0.1"))).isTrue();

        assertThat(new RateLimitFilter(new LocalRateLimiterFactory(), new RateLimitProperties(), json)
                .shouldNotFilter(request(SECKILL_PATH, "10.0.0.1"))).isTrue();
    }
}
