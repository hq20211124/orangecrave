package com.orangecrave.admin.common.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 缓存全局参数。TTL 是「失效只达本进程」时的兜底上限：多实例部署下，
 * 一个节点改了字典、其他节点最多在 TTL 内看到旧值。
 */
@Data
@Component
@ConfigurationProperties(prefix = "orangecrave.cache")
public class CacheProperties {

    /** 缓存存活秒数（兜底上限）。*/
    private long ttlSeconds = 60;

    /** 单个缓存的条目上限，超过即整体清空（字典表条目极少）。*/
    private int maxSize = 1024;

    public long ttlMillis() {
        return ttlSeconds * 1000L;
    }
}
