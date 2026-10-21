package com.orangecrave.admin.order.async;

import com.orangecrave.admin.common.cache.Cache;
import com.orangecrave.admin.common.cache.CacheFactory;
import org.springframework.stereotype.Component;

/**
 * 异步下单结果的存储（见 03 篇 §2.6）。订单落库在消费者线程，客户端在别的连接上等结果，
 * 两者需要一个共享位置交接——本机缓存（单实例）或 Redis（多实例，任一台都能答）都由
 * {@link CacheFactory} 决定，这里只管读写。
 *
 * <p>结果带 TTL：它只在客户端轮询期间有意义，过期即回收，不必人工清理。
 */
@Component
public class AsyncOrderResultStore {

    private static final String NAMESPACE = "order-async-result";

    /** 结果保留时长（秒）：覆盖客户端轮询窗口即可。 */
    private static final long RESULT_TTL_SECONDS = 600;

    private final Cache<String, AsyncOrderResult> cache;

    public AsyncOrderResultStore(CacheFactory cacheFactory) {
        this.cache = cacheFactory.create(NAMESPACE, RESULT_TTL_SECONDS);
    }

    public void markQueued(String requestId) {
        cache.put(requestId, AsyncOrderResult.queued());
    }

    public void complete(String requestId, Long orderId) {
        cache.put(requestId, AsyncOrderResult.success(orderId));
    }

    public void fail(String requestId, String message) {
        cache.put(requestId, AsyncOrderResult.failed(message));
    }

    /** 未知或已过期的 requestId 返回 null，调用方据此提示「结果不存在或已过期」。 */
    public AsyncOrderResult get(String requestId) {
        return cache.get(requestId, key -> null);
    }
}
