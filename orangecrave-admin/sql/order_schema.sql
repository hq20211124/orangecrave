-- ============================================================
-- order 模块 DDL（01 篇 §5）
-- ============================================================

CREATE TABLE IF NOT EXISTS oms_order (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_no          VARCHAR(32) NOT NULL,
    user_id           BIGINT DEFAULT NULL,
    order_type        TINYINT NOT NULL DEFAULT 1 COMMENT '1 普通 / 2 打样 / 3 无料领取 / 4 现货',
    status            TINYINT NOT NULL DEFAULT 0,
    payer_user_id     BIGINT DEFAULT NULL COMMENT '实际付款人',
    total_amount      DECIMAL(12,2) DEFAULT 0.00,
    freight_amount    DECIMAL(12,2) DEFAULT 0.00,
    discount_amount   DECIMAL(12,2) DEFAULT 0.00,
    pay_amount        DECIMAL(12,2) DEFAULT 0.00,
    refund_amount     DECIMAL(12,2) DEFAULT 0.00,
    coupon_id         BIGINT DEFAULT NULL,
    group_id          BIGINT DEFAULT NULL,
    share_code_id     BIGINT DEFAULT NULL,
    activity_id       BIGINT DEFAULT NULL,
    channel_id        BIGINT DEFAULT NULL,
    pay_time          DATETIME DEFAULT NULL,
    pay_type          TINYINT DEFAULT NULL,
    transaction_id    VARCHAR(64) DEFAULT NULL,
    expire_time       DATETIME DEFAULT NULL COMMENT '待付款超时点',
    receiver_snapshot JSON COMMENT '收货地址快照',
    remark            VARCHAR(255) DEFAULT NULL,
    work_order_no     VARCHAR(64) DEFAULT NULL COMMENT 'MES 工单号',
    expected_delivery DATETIME DEFAULT NULL COMMENT '预计交期（工厂日历）',
    sync_time         DATETIME DEFAULT NULL COMMENT 'MES 状态最近同步时间',
    create_time       DATETIME DEFAULT NULL,
    update_time       DATETIME DEFAULT NULL,
    deleted           TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_status (user_id, status),
    KEY idx_status_expire (status, expire_time),
    KEY idx_pay_time (pay_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单主表';

CREATE TABLE IF NOT EXISTS oms_order_item (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id         BIGINT NOT NULL,
    order_no         VARCHAR(32) DEFAULT NULL,
    spu_id           BIGINT DEFAULT NULL,
    sku_id           BIGINT DEFAULT NULL,
    goods_name       VARCHAR(200) DEFAULT NULL,
    sku_spec         VARCHAR(200) DEFAULT NULL,
    image            VARCHAR(255) DEFAULT NULL,
    work_id          BIGINT DEFAULT NULL,
    activity_id      BIGINT DEFAULT NULL COMMENT '本行活动（秒杀按行抢配额，取消/退款按行归还）',
    qty              INT DEFAULT 0,
    unit_price       DECIMAL(12,2) DEFAULT 0.00,
    total_price      DECIMAL(12,2) DEFAULT 0.00,
    craft_snapshot   JSON,
    pricing_snapshot JSON,
    create_time      DATETIME DEFAULT NULL,
    update_time      DATETIME DEFAULT NULL,
    deleted          TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单明细';

CREATE TABLE IF NOT EXISTS oms_order_image (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_item_id BIGINT NOT NULL,
    slot_index    INT DEFAULT 0,
    image_url     VARCHAR(500) DEFAULT NULL,
    copies        INT DEFAULT 1,
    pHash         BIGINT DEFAULT NULL,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_item (order_item_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单图片（图槽位）';

CREATE TABLE IF NOT EXISTS oms_order_status_log (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id      BIGINT NOT NULL,
    from_status   INT DEFAULT NULL,
    to_status     INT DEFAULT NULL,
    operator_type VARCHAR(20) DEFAULT NULL,
    operator_id   BIGINT DEFAULT NULL,
    remark        VARCHAR(255) DEFAULT NULL,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单状态流转日志';

CREATE TABLE IF NOT EXISTS oms_delivery (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id         BIGINT NOT NULL,
    delivery_company VARCHAR(50) DEFAULT NULL,
    delivery_no      VARCHAR(64) DEFAULT NULL,
    delivery_time    DATETIME DEFAULT NULL,
    receive_time     DATETIME DEFAULT NULL,
    status           TINYINT DEFAULT 1 COMMENT '1 已发货 / 2 已签收 / 3 退回',
    create_time      DATETIME DEFAULT NULL,
    update_time      DATETIME DEFAULT NULL,
    deleted          TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_delivery_no (delivery_no),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='发货';

CREATE TABLE IF NOT EXISTS oms_aftersale (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id      BIGINT NOT NULL,
    order_item_id BIGINT DEFAULT NULL,
    aftersale_no  VARCHAR(32) NOT NULL,
    type          TINYINT DEFAULT 1 COMMENT '1 退款 / 2 退货退款 / 3 色差 / 4 其他',
    reason        VARCHAR(255) DEFAULT NULL,
    description   VARCHAR(500) DEFAULT NULL,
    images        TEXT,
    amount        DECIMAL(12,2) DEFAULT 0.00,
    status        TINYINT DEFAULT 1 COMMENT '1 待审 / 2 通过 / 3 驳回 / 4 完成',
    audit_user_id BIGINT DEFAULT NULL,
    audit_time    DATETIME DEFAULT NULL,
    audit_remark  VARCHAR(255) DEFAULT NULL,
    refund_no     VARCHAR(32) DEFAULT NULL,
    refund_time   DATETIME DEFAULT NULL,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_aftersale_no (aftersale_no),
    KEY idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='售后';

CREATE TABLE IF NOT EXISTS oms_invoice (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    invoice_no VARCHAR(32) NOT NULL,
    user_id    BIGINT DEFAULT NULL,
    order_ids  VARCHAR(255) DEFAULT NULL,
    title_type TINYINT DEFAULT 1 COMMENT '1 个人 / 2 企业',
    title      VARCHAR(100) DEFAULT NULL,
    tax_no     VARCHAR(100) DEFAULT NULL,
    amount     DECIMAL(12,2) DEFAULT 0.00,
    email      VARCHAR(100) DEFAULT NULL,
    type       TINYINT DEFAULT 1 COMMENT '1 电子 / 2 纸质',
    status     TINYINT DEFAULT 1 COMMENT '1 待审 / 2 已开 / 3 驳回 / 4 已取消',
    file_url   VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_no (invoice_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='发票';
