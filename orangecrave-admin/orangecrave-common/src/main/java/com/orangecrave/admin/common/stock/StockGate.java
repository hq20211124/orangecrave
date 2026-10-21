package com.orangecrave.admin.common.stock;

/**
 * 限量库存预扣闸门（见 04 篇 §3.3）。「少量配额 + 海量并发」的场景里，把配额判断放到内存中
 * 原子完成，绝大多数抢不到的请求在触达数据库之前就被挡掉，数据库只承接真正的赢家。
 *
 * <p>数据库的条件 UPDATE 仍是最终账本：预扣只是前置过滤器，它漏掉或状态失真时，
 * 落库那一步照样能挡住超卖。Redis 关闭时由 {@link NoopStockGate} 接管，一律放行，
 * 判定全部交给数据库，行为与未引入 Redis 时完全一致。
 *
 * <p>key 为业务自定的命名空间（如 {@code seckill:12}），实现负责拼上自己的前缀。
 */
public interface StockGate {

    /**
     * 尝试预扣。total 用于首次把配额从数据库同步到内存（幂等，可重复调）。
     *
     * @return true 表示预扣成功、可继续落库；false 表示已抢完
     */
    boolean tryDeduct(String key, int total, int qty);

    /** 归还预扣：落库失败回滚、订单取消 / 退款时都要调，否则配额被死单占住。 */
    void restore(String key, int qty);
}
