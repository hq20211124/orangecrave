SET NAMES utf8mb4;
-- ============================================================
-- service 模块 DDL（01 篇 §8）
-- ============================================================

CREATE TABLE IF NOT EXISTS svc_ticket (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_no  VARCHAR(32) NOT NULL,
    user_id    BIGINT DEFAULT NULL,
    type       TINYINT DEFAULT 1 COMMENT '1 售后咨询 / 2 侵权投诉 / 3 色差投诉 / 4 其他',
    order_id   BIGINT DEFAULT NULL COMMENT '可空',
    title      VARCHAR(200) DEFAULT NULL,
    content    TEXT,
    images     TEXT,
    status     TINYINT DEFAULT 1 COMMENT '1 待受理 / 2 处理中 / 3 已解决 / 4 已关闭',
    handler_id BIGINT DEFAULT NULL,
    source     TINYINT DEFAULT 1 COMMENT '1 用户提交 / 2 客服代建',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='客服工单';

CREATE TABLE IF NOT EXISTS svc_ticket_reply (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_id  BIGINT NOT NULL,
    reply_type TINYINT DEFAULT 1 COMMENT '1 客服 / 2 用户',
    user_id    BIGINT DEFAULT NULL,
    content    TEXT,
    images     TEXT,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_ticket (ticket_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工单回复';
