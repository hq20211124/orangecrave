package com.orangecrave.admin.common.mq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 消息与消费者参数（见 application.yml 的 {@code orangecrave.mq}）。仅 Redis Stream 实现使用；
 * 同步直投实现不需要任何配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "orangecrave.mq")
public class MessageProperties {

    /** Stream 键前缀。 */
    private String streamPrefix = "orangecrave:stream:";

    /** 消费者组名：多实例共用同一组，一条消息只会被一个实例消费。 */
    private String group = "orangecrave";

    /**
     * 消费者名。留空则用「主机名 + 进程号」自动生成——多实例部署时不必手工为每台机器起名，
     * 也避免两台机器共用同一个消费者名导致 pending 归属混乱。
     */
    private String consumerName = "";

    /** 单批拉取条数。 */
    private int batchSize = 16;

    /** 单次阻塞等待时长（毫秒）：有数据立即返回，无数据等这么久再空转下一轮。 */
    private long blockMillis = 1000;

    /** Stream 最大长度：消费完一批后裁剪，防止 Stream 无限增长。 */
    private long maxLength = 10_000;

    /** 未建立 Stream 时的空转间隔（毫秒），避免在「组还不存在」时打满 CPU。 */
    private long idleSleepMillis = 500;

    /** 启动后首次拉取的延迟（毫秒），等容器装配完再触发监听器解析。 */
    private long initialDelayMillis = 3000;
}
