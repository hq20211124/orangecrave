package com.orangecrave.admin.common.limit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 限流策略配置（见 application.yml 的 {@code orangecrave.rate-limit}）。
 *
 * <p>键是**精确路径**，或以 {@code /**} 结尾表示「整棵子树」。默认精确匹配是有意的：
 * 秒杀提交与结果轮询是相邻路径（{@code /order/seckill} 与 {@code /order/seckill/result/{id}}），
 * 若按前缀匹配，轮询会被提交的令牌桶一起限住——而轮询是客户端主动节制的读请求，不该被限。
 *
 * <p>两类规则同时命中时取更长者，因此「宽的子树 + 单个精确收口」可以共存。
 * 未命中任何规则的路径不限流：限流只在明确有压力的入口开启，不对全站一刀切。
 */
@Data
@Component
@ConfigurationProperties(prefix = "orangecrave.rate-limit")
public class RateLimitProperties {

    /** 总开关。关掉后过滤器直接放行（排查线上问题时第一件事就是把它关掉）。 */
    private boolean enabled = true;

    /** 路径（精确，或以 {@code /**} 结尾的子树）→ 策略。 */
    private Map<String, Rule> rules = new LinkedHashMap<>();

    @Data
    public static class Rule {

        /** 令牌补充速率（个/秒）。 */
        private int permitsPerSecond = 20;

        /** 桶容量，即允许的瞬时突发量。 */
        private int burst = 40;
    }
}
