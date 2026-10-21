package com.orangecrave.admin.common.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.orangecrave.admin.common.entity.BaseEntity;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * 请求体反序列化护栏（mass-assignment 止血）。后台多数增改接口直接绑定实体，
 * 客户端只要多传 deleted / createTime / updateTime 就能改写这三列，
 * 逻辑删除位被篡改尤其难查。这三列只由框架与数据库决定，反序列化时一律忽略。
 * 根治办法是每个接口配独立 DTO（见 02 篇 §1.4），在完成之前先堵这条通道。 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer ignoreAuditFieldsOnRequestBody() {
        // MixIn 按类层级生效：对所有继承 BaseEntity 的实体都起作用，响应仍可正常输出这三列
        return builder -> builder.mixIn(BaseEntity.class, AuditFieldMixIn.class);
    }

    abstract static class AuditFieldMixIn {

        @JsonProperty(value = "deleted", access = JsonProperty.Access.READ_ONLY)
        abstract Integer getDeleted();

        @JsonProperty(value = "createTime", access = JsonProperty.Access.READ_ONLY)
        abstract LocalDateTime getCreateTime();

        @JsonProperty(value = "updateTime", access = JsonProperty.Access.READ_ONLY)
        abstract LocalDateTime getUpdateTime();
    }
}
