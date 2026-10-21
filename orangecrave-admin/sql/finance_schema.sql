-- ============================================================
-- finance 模块 DDL（01 篇 §9）
-- ============================================================

CREATE TABLE IF NOT EXISTS fin_refund_ledger (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    refund_no   VARCHAR(32) NOT NULL,
    biz_type    TINYINT DEFAULT 1 COMMENT '1 订单退款 / 2 拼团失败退款 / 3 无料预付款退回 / 4 售后',
    biz_id      BIGINT DEFAULT NULL,
    order_id    BIGINT DEFAULT NULL,
    user_id     BIGINT DEFAULT NULL,
    amount      DECIMAL(12,2) DEFAULT 0.00,
    channel     TINYINT DEFAULT 1 COMMENT '1 微信原路退回',
    status      TINYINT DEFAULT 1 COMMENT '1 处理中 / 2 成功 / 3 失败',
    -- 03 篇 §3.3：管理员强制退款两段式，钱要等财务复核后才动
    review_status TINYINT NOT NULL DEFAULT 0 COMMENT '0 无需复核 / 1 待复核 / 2 复核通过 / 3 复核驳回',
    reviewer_id   BIGINT DEFAULT NULL COMMENT '复核人 sys_user.id',
    review_time   DATETIME DEFAULT NULL,
    review_remark VARCHAR(255) DEFAULT NULL COMMENT '复核意见',
    remark      VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='退款流水';

CREATE TABLE IF NOT EXISTS fin_daily_settlement (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    settle_date    DATE DEFAULT NULL,
    order_amount   DECIMAL(14,2) DEFAULT 0.00 COMMENT '支付口径',
    groupon_amount DECIMAL(14,2) DEFAULT 0.00 COMMENT '成团口径',
    refund_amount  DECIMAL(14,2) DEFAULT 0.00,
    net_amount     DECIMAL(14,2) DEFAULT 0.00,
    order_count    INT DEFAULT 0,
    create_time    DATETIME DEFAULT NULL,
    update_time    DATETIME DEFAULT NULL,
    deleted        TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_settle_date (settle_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='日结';
