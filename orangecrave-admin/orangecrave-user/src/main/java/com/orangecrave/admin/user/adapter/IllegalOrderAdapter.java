package com.orangecrave.admin.user.adapter;

/**
 * 身份认证被驳回时清理该用户名下的非法订单（03 篇 §2.1 状态 13）。
 * 端口放在 user，由 order 实现：user 不认识订单，order 已经依赖 user，方向不会成环。 */
public interface IllegalOrderAdapter {

    /** 把该用户仍处于待付款/待成团/待审核的订单改判为非法取消，返回处理条数。 */
    int cancelIllegalOrders(Long userId, String remark);
}
