package com.orangecrave.admin.common.stock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 无 Redis 时的闸门实现：一律放行，判定完全交给数据库条件 UPDATE。
 * 这不是「空实现凑数」——条件 UPDATE 本身就能防超卖，只是每笔请求都要碰一次数据库。
 */
@Component
@ConditionalOnProperty(prefix = "orangecrave.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopStockGate implements StockGate {

    @Override
    public boolean tryDeduct(String key, int total, int qty) {
        return true;
    }

    @Override
    public void restore(String key, int qty) {
        // 从未扣过，无需归还
    }
}
