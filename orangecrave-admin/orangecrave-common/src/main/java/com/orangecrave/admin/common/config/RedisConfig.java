package com.orangecrave.admin.common.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.orangecrave.admin.common.lock.DistributedLock;
import com.orangecrave.admin.common.lock.RedisDistributedLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 底座装配（见 00 篇支撑层 + 03 篇）。整类挂在 {@code orangecrave.redis.enabled=true} 开关后，
 * 默认关闭：本地与测试都不需要外部 Redis，相关实现由 {@code LocalCacheFactory} /
 * {@code LocalDistributedLock} 兜底。
 *
 * <p>这里统一提供三样运行时共享能力：① Sa-Token 会话 dao（多实例登录态共享，哪台机器登录都算登录）；
 * ② 分布式锁（定时兜底任务只让一个节点跑）；③ 值走 JSON 的 RedisTemplate（供跨实例缓存）。
 */
@Configuration
@ConditionalOnProperty(prefix = "orangecrave.redis", name = "enabled", havingValue = "true")
public class RedisConfig {

    /**
     * 值序列化用 JSON 而非 JDK 序列化：键可读、可跨语言、类结构小改不易整段失效。
     * 显式注册 JavaTimeModule——实体带 LocalDateTime（createTime/updateTime），
     * 默认 Jackson 不认它。 */
    @Bean("orangecraveRedisTemplate")
    public RedisTemplate<String, Object> orangecraveRedisTemplate(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 会话 dao 落 Redis。{@link SaTokenDaoRedisJackson} 是官方参考实现，这里只是显式装配它
     * （其自带自动配置已在 application.yml 里 exclude，避免它无视本开关去要求 RedisConnectionFactory）。 */
    @Bean
    public SaTokenDao saTokenDao(RedisConnectionFactory connectionFactory) {
        SaTokenDaoRedisJackson dao = new SaTokenDaoRedisJackson();
        dao.init(connectionFactory);
        return dao;
    }

    @Bean
    public DistributedLock distributedLock(StringRedisTemplate stringRedisTemplate) {
        return new RedisDistributedLock(stringRedisTemplate, "orangecrave:lock:");
    }
}
