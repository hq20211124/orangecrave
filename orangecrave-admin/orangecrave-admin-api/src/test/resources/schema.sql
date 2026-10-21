-- H2（MODE=MySQL）测试用 schema —— system 模块
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    password        VARCHAR(100) NOT NULL,
    nickname        VARCHAR(50),
    phone           VARCHAR(20),
    status          TINYINT DEFAULT 1,
    last_login_time TIMESTAMP,
    last_login_ip   VARCHAR(50),
    create_time     TIMESTAMP,
    update_time     TIMESTAMP,
    deleted         TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_username ON sys_user (username);

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(50),
    role_key    VARCHAR(50),
    remark      VARCHAR(255),
    status      TINYINT DEFAULT 1,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_key ON sys_role (role_key);

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id   BIGINT NOT NULL DEFAULT 0,
    menu_name   VARCHAR(50) NOT NULL,
    menu_type   TINYINT NOT NULL,
    path        VARCHAR(200),
    perm_key    VARCHAR(100),
    icon        VARCHAR(100),
    sort        INT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id     BIGINT NOT NULL,
    menu_id     BIGINT NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    oper_module    VARCHAR(50),
    oper_type      VARCHAR(20),
    oper_desc      VARCHAR(255),
    method         VARCHAR(200),
    request_url    VARCHAR(255),
    request_param  TEXT,
    before_data    TEXT,
    after_data     TEXT,
    oper_user_id   BIGINT,
    oper_user_name VARCHAR(50),
    oper_ip        VARCHAR(50),
    oper_time      TIMESTAMP,
    status         TINYINT DEFAULT 1,
    error_msg      TEXT,
    create_time    TIMESTAMP,
    update_time    TIMESTAMP,
    deleted        TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_login_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name   VARCHAR(50),
    ip          VARCHAR(50),
    browser     VARCHAR(255),
    os          VARCHAR(100),
    status      TINYINT DEFAULT 1,
    msg         VARCHAR(255),
    login_time  TIMESTAMP,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted     TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key   VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_type  TINYINT DEFAULT 1,
    remark       VARCHAR(255),
    create_time  TIMESTAMP,
    update_time  TIMESTAMP,
    deleted      TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_config_key ON sys_config (config_key);

-- task / order / finance（供完整上下文启动，H2 版）
CREATE TABLE IF NOT EXISTS tsk_delay_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_type VARCHAR(50) NOT NULL,
    biz_id BIGINT NOT NULL,
    fire_time TIMESTAMP,
    exec_time TIMESTAMP,
    status TINYINT DEFAULT 1,
    retry_count INT DEFAULT 0,
    error_msg TEXT,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_type_biz ON tsk_delay_log (task_type, biz_id);

CREATE TABLE IF NOT EXISTS oms_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT, order_type TINYINT DEFAULT 1, status TINYINT DEFAULT 0,
    payer_user_id BIGINT,
    total_amount DECIMAL(12,2), freight_amount DECIMAL(12,2), discount_amount DECIMAL(12,2),
    pay_amount DECIMAL(12,2), refund_amount DECIMAL(12,2),
    coupon_id BIGINT, group_id BIGINT, share_code_id BIGINT, activity_id BIGINT, channel_id BIGINT,
    pay_time TIMESTAMP, pay_type TINYINT, transaction_id VARCHAR(64),
    expire_time TIMESTAMP, receiver_snapshot TEXT, remark VARCHAR(255),
    work_order_no VARCHAR(64), expected_delivery TIMESTAMP, sync_time TIMESTAMP,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_order_no ON oms_order (order_no);

CREATE TABLE IF NOT EXISTS oms_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL, order_no VARCHAR(32),
    spu_id BIGINT, sku_id BIGINT, goods_name VARCHAR(200), sku_spec VARCHAR(200), image VARCHAR(255),
    work_id BIGINT, activity_id BIGINT, qty INT, unit_price DECIMAL(12,2), total_price DECIMAL(12,2),
    craft_snapshot TEXT, pricing_snapshot TEXT,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS oms_order_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL, slot_index INT, image_url VARCHAR(500), copies INT, pHash BIGINT,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS oms_order_status_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL, from_status INT, to_status INT,
    operator_type VARCHAR(20), operator_id BIGINT, remark VARCHAR(255),
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS oms_delivery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL, delivery_company VARCHAR(50), delivery_no VARCHAR(64),
    delivery_time TIMESTAMP, receive_time TIMESTAMP, status TINYINT DEFAULT 1,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS oms_aftersale (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL, order_item_id BIGINT, aftersale_no VARCHAR(32) NOT NULL,
    type TINYINT DEFAULT 1, reason VARCHAR(255), description VARCHAR(500), images TEXT,
    amount DECIMAL(12,2), status TINYINT DEFAULT 1,
    audit_user_id BIGINT, audit_time TIMESTAMP, audit_remark VARCHAR(255),
    refund_no VARCHAR(32), refund_time TIMESTAMP,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS oms_invoice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(32) NOT NULL, user_id BIGINT, order_ids VARCHAR(255),
    title_type TINYINT DEFAULT 1, title VARCHAR(100), tax_no VARCHAR(100),
    amount DECIMAL(12,2), email VARCHAR(100), type TINYINT DEFAULT 1, status TINYINT DEFAULT 1,
    file_url VARCHAR(255), create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS fin_refund_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_no VARCHAR(32) NOT NULL, biz_type TINYINT DEFAULT 1, biz_id BIGINT,
    order_id BIGINT, user_id BIGINT, amount DECIMAL(12,2), channel TINYINT DEFAULT 1,
    status TINYINT DEFAULT 1, remark VARCHAR(255),
    review_status TINYINT NOT NULL DEFAULT 0, reviewer_id BIGINT, review_time TIMESTAMP, review_remark VARCHAR(255),
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

-- user / goods（供端到端链路测试，H2 版）
CREATE TABLE IF NOT EXISTS ums_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) NOT NULL, unionid VARCHAR(64), nickname VARCHAR(100), avatar VARCHAR(255),
    phone VARCHAR(20), identity_type TINYINT DEFAULT 0, cert_status TINYINT DEFAULT 0,
    channel_id BIGINT, vip_level TINYINT DEFAULT 0, points INT DEFAULT 0,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_name VARCHAR(100), channel_code VARCHAR(50), status TINYINT DEFAULT 1,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ums_identity_cert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT, identity_type TINYINT, real_name VARCHAR(50), id_card_no VARCHAR(255),
    material_urls TEXT, status TINYINT DEFAULT 1, audit_user_id BIGINT, audit_time TIMESTAMP,
    audit_remark VARCHAR(255), proof_coupon_granted TINYINT DEFAULT 0,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pms_series (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    series_name VARCHAR(100) NOT NULL, cover VARCHAR(255), ip_name VARCHAR(100),
    sort INT DEFAULT 0, status TINYINT DEFAULT 1,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pms_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL, parent_id BIGINT DEFAULT 0,
    sort INT DEFAULT 0, status TINYINT DEFAULT 1,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pms_spu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spu_name VARCHAR(200) NOT NULL, sub_title VARCHAR(255), cover VARCHAR(255), images TEXT,
    series_id BIGINT, category_id BIGINT, goods_type TINYINT DEFAULT 1, pricing_type TINYINT DEFAULT 1,
    base_price DECIMAL(12,2) DEFAULT 0, area_price DECIMAL(12,2) DEFAULT 0, pricing_config TEXT,
    designer_id BIGINT, min_order_qty INT DEFAULT 1, increment_qty INT DEFAULT 1,
    freight_template_id BIGINT, need_proof TINYINT DEFAULT 0, status TINYINT DEFAULT 1,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pms_sku (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spu_id BIGINT NOT NULL, sku_code VARCHAR(64) NOT NULL, spec_value_ids TEXT,
    price DECIMAL(12,2) DEFAULT 0, stock INT DEFAULT 0, weight DECIMAL(10,2) DEFAULT 0,
    image VARCHAR(255), length_mm INT, width_mm INT, status TINYINT DEFAULT 1,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pms_craft (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    craft_name VARCHAR(50) NOT NULL, craft_type VARCHAR(50), add_price DECIMAL(12,2) DEFAULT 0,
    add_price_type TINYINT DEFAULT 1, status TINYINT DEFAULT 1,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pms_sku_craft (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id BIGINT NOT NULL, craft_id BIGINT NOT NULL,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pms_freight_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL, charge_type TINYINT DEFAULT 1,
    first_unit INT DEFAULT 1, first_fee DECIMAL(12,2) DEFAULT 0,
    extra_unit INT DEFAULT 1, extra_fee DECIMAL(12,2) DEFAULT 0,
    free_threshold DECIMAL(12,2), region_rules TEXT,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

-- user 收货地址 / marketing 邀请（新增接口验证用，H2 版）
CREATE TABLE IF NOT EXISTS ums_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT, receiver_name VARCHAR(50), receiver_phone VARCHAR(20),
    province VARCHAR(50), city VARCHAR(50), district VARCHAR(50), detail VARCHAR(255),
    is_default TINYINT DEFAULT 0,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mkt_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_name VARCHAR(100), activity_type TINYINT,
    spu_ids TEXT, sku_ids TEXT,
    start_time TIMESTAMP, end_time TIMESTAMP, rule_config TEXT,
    status TINYINT DEFAULT 1,
    total_stock INT, sold_stock INT NOT NULL DEFAULT 0,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mkt_invite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT, inviter_user_id BIGINT, invitee_user_id BIGINT, order_id BIGINT,
    status TINYINT DEFAULT 1, inviter_reward_granted TINYINT DEFAULT 0, invitee_reward_granted TINYINT DEFAULT 0,
    create_time TIMESTAMP, update_time TIMESTAMP, deleted TINYINT DEFAULT 0
);

-- ============================================================
-- 以下 20 张表由 sql/*_schema.sql 适配而来（去 UNSIGNED / COMMENT / ENGINE，KEY 转独立索引）
-- ============================================================
CREATE TABLE IF NOT EXISTS aud_image_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_type      TINYINT NOT NULL DEFAULT 1,
    biz_id        BIGINT DEFAULT NULL,
    image_url     VARCHAR(500) DEFAULT NULL,
    pHash         BIGINT DEFAULT NULL,
    auto_result   TINYINT DEFAULT NULL,
    auto_remark   VARCHAR(255) DEFAULT NULL,
    manual_result TINYINT DEFAULT NULL,
    audit_user_id BIGINT DEFAULT NULL,
    audit_time    DATETIME DEFAULT NULL,
    audit_remark  VARCHAR(255) DEFAULT NULL,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_auto_result ON aud_image_audit (auto_result);

CREATE TABLE IF NOT EXISTS aud_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pHash       BIGINT DEFAULT NULL,
    source      VARCHAR(50) DEFAULT NULL,
    remark      VARCHAR(255) DEFAULT NULL,
    status      TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_pHash ON aud_blacklist (pHash);

CREATE TABLE IF NOT EXISTS aud_sensitive_word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word        VARCHAR(100) DEFAULT NULL,
    category    VARCHAR(50) DEFAULT NULL,
    action      VARCHAR(20) DEFAULT NULL,
    status      TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS aud_copyright_promise (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT DEFAULT NULL,
    biz_type        TINYINT DEFAULT NULL,
    biz_id          BIGINT DEFAULT NULL,
    promise_version VARCHAR(20) DEFAULT NULL,
    ip              VARCHAR(50) DEFAULT NULL,
    create_time     DATETIME DEFAULT NULL,
    update_time     DATETIME DEFAULT NULL,
    deleted         TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_user ON aud_copyright_promise (user_id);

CREATE TABLE IF NOT EXISTS fin_daily_settlement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settle_date    DATE DEFAULT NULL,
    order_amount   DECIMAL(14,2) DEFAULT 0.00,
    groupon_amount DECIMAL(14,2) DEFAULT 0.00,
    refund_amount  DECIMAL(14,2) DEFAULT 0.00,
    net_amount     DECIMAL(14,2) DEFAULT 0.00,
    order_count    INT DEFAULT 0,
    create_time    DATETIME DEFAULT NULL,
    update_time    DATETIME DEFAULT NULL,
    deleted        TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_settle_date ON fin_daily_settlement (settle_date);

CREATE TABLE IF NOT EXISTS pms_spec_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spu_id      BIGINT NOT NULL,
    group_name  VARCHAR(50) DEFAULT NULL,
    sort        INT DEFAULT 0,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_spu ON pms_spec_group (spu_id);

CREATE TABLE IF NOT EXISTS pms_spec_value (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id   BIGINT NOT NULL,
    value_name VARCHAR(50) DEFAULT NULL,
    sort       INT DEFAULT 0,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_group ON pms_spec_value (group_id);

CREATE TABLE IF NOT EXISTS pms_designer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    designer_name VARCHAR(100) NOT NULL,
    avatar        VARCHAR(255) DEFAULT NULL,
    intro         VARCHAR(255) DEFAULT NULL,
    status        TINYINT DEFAULT 1,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS pms_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(100) NOT NULL,
    designer_id   BIGINT DEFAULT NULL,
    category_id   BIGINT DEFAULT NULL,
    preview_url   VARCHAR(255) DEFAULT NULL,
    source_url    VARCHAR(255) DEFAULT NULL,
    status        TINYINT DEFAULT 1,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mkt_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id    BIGINT DEFAULT NULL,
    leader_user_id BIGINT DEFAULT NULL,
    target_size    INT DEFAULT 3,
    current_size   INT DEFAULT 1,
    status         TINYINT DEFAULT 1,
    expire_time    DATETIME DEFAULT NULL,
    is_simulated   TINYINT DEFAULT 0,
    create_time    DATETIME DEFAULT NULL,
    update_time    DATETIME DEFAULT NULL,
    deleted        TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_status_expire ON mkt_group (status, expire_time);

CREATE TABLE IF NOT EXISTS mkt_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id   BIGINT NOT NULL,
    user_id    BIGINT DEFAULT NULL,
    order_id   BIGINT DEFAULT NULL,
    is_leader  TINYINT DEFAULT 0,
    is_virtual TINYINT DEFAULT 0,
    join_time  DATETIME DEFAULT NULL,
    status     TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_group ON mkt_group_member (group_id);

CREATE TABLE IF NOT EXISTS mkt_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT DEFAULT NULL,
    coupon_name VARCHAR(100) DEFAULT NULL,
    coupon_type TINYINT DEFAULT 1,
    threshold   DECIMAL(12,2) DEFAULT 0.00,
    amount      DECIMAL(12,2) DEFAULT 0.00,
    discount    DECIMAL(10,2) DEFAULT NULL,
    valid_days  INT DEFAULT 30,
    total_qty   INT DEFAULT 0,
    issued_qty  INT DEFAULT 0,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS mkt_coupon_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id   BIGINT NOT NULL,
    user_id     BIGINT DEFAULT NULL,
    code        VARCHAR(32) DEFAULT NULL,
    status      TINYINT DEFAULT 1,
    use_time    DATETIME DEFAULT NULL,
    order_id    BIGINT DEFAULT NULL,
    expire_time DATETIME DEFAULT NULL,
    source      TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_user ON mkt_coupon_user (user_id);

CREATE TABLE IF NOT EXISTS svc_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no  VARCHAR(32) NOT NULL,
    user_id    BIGINT DEFAULT NULL,
    type       TINYINT DEFAULT 1,
    order_id   BIGINT DEFAULT NULL,
    title      VARCHAR(200) DEFAULT NULL,
    content    TEXT,
    images     TEXT,
    status     TINYINT DEFAULT 1,
    handler_id BIGINT DEFAULT NULL,
    source     TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_ticket_no ON svc_ticket (ticket_no);

CREATE TABLE IF NOT EXISTS svc_ticket_reply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id  BIGINT NOT NULL,
    reply_type TINYINT DEFAULT 1,
    user_id    BIGINT DEFAULT NULL,
    content    TEXT,
    images     TEXT,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_ticket ON svc_ticket_reply (ticket_id);

CREATE TABLE IF NOT EXISTS wms_work (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT DEFAULT NULL,
    work_name      VARCHAR(100) DEFAULT NULL,
    cover_url      VARCHAR(255) DEFAULT NULL,
    spu_id         BIGINT DEFAULT NULL,
    sku_id         BIGINT DEFAULT NULL,
    status         TINYINT DEFAULT NULL,
    audit_remark   VARCHAR(255) DEFAULT NULL,
    expire_time    DATETIME DEFAULT NULL,
    source_share_id BIGINT DEFAULT NULL,
    deleted_shared TINYINT DEFAULT 0,
    create_time    DATETIME DEFAULT NULL,
    update_time    DATETIME DEFAULT NULL,
    deleted        TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_user ON wms_work (user_id);

CREATE TABLE IF NOT EXISTS wms_work_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_id    BIGINT NOT NULL,
    slot_index INT DEFAULT 0,
    image_url  VARCHAR(500) DEFAULT NULL,
    thumb_url  VARCHAR(500) DEFAULT NULL,
    copies     INT DEFAULT 1,
    pHash      BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_work ON wms_work_image (work_id);

CREATE TABLE IF NOT EXISTS wms_collection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT DEFAULT NULL,
    collection_name VARCHAR(100) DEFAULT NULL,
    cover_url       VARCHAR(255) DEFAULT NULL,
    status          TINYINT DEFAULT NULL,
    create_time     DATETIME DEFAULT NULL,
    update_time     DATETIME DEFAULT NULL,
    deleted         TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS wms_collection_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collection_id BIGINT NOT NULL,
    work_id       BIGINT NOT NULL,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_collection ON wms_collection_item (collection_id);

CREATE TABLE IF NOT EXISTS wms_share_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    share_code      VARCHAR(32) NOT NULL,
    user_id         BIGINT DEFAULT NULL,
    work_id         BIGINT DEFAULT NULL,
    collection_id   BIGINT DEFAULT NULL,
    share_type      TINYINT DEFAULT 1,
    payer_type      TINYINT DEFAULT 1,
    max_order_qty   INT DEFAULT 0,
    used_qty        INT DEFAULT 0,
    expire_time     DATETIME DEFAULT NULL,
    status          TINYINT DEFAULT 1,
    invalid_reason  VARCHAR(100) DEFAULT NULL,
    prepay_amount   DECIMAL(12,2) DEFAULT 0.00,
    prepay_used     DECIMAL(12,2) DEFAULT 0.00,
    prepay_refunded DECIMAL(12,2) DEFAULT 0.00,
    create_time     DATETIME DEFAULT NULL,
    update_time     DATETIME DEFAULT NULL,
    deleted         TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_share_code ON wms_share_code (share_code);
CREATE INDEX IF NOT EXISTS idx_status_expire ON wms_share_code (status, expire_time);


CREATE TABLE IF NOT EXISTS sys_idempotency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idem_key      VARCHAR(64)  NOT NULL,
    api           VARCHAR(200) NOT NULL,
    request_hash  VARCHAR(64)  DEFAULT NULL,
    response_body MEDIUMTEXT,
    status        TINYINT      NOT NULL DEFAULT 1,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_key_api ON sys_idempotency (idem_key, api);
CREATE INDEX IF NOT EXISTS idx_create_time ON sys_idempotency (create_time);
