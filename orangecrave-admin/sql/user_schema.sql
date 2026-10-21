-- ============================================================
-- user 模块 DDL（01 篇 §2）
-- ============================================================

CREATE TABLE IF NOT EXISTS ums_user (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    openid        VARCHAR(64) NOT NULL,
    unionid       VARCHAR(64) DEFAULT NULL,
    nickname      VARCHAR(100) DEFAULT NULL,
    avatar        VARCHAR(255) DEFAULT NULL,
    phone         VARCHAR(20) DEFAULT NULL,
    identity_type TINYINT DEFAULT 0 COMMENT '0 普通 / 1 团长 / 2 摊主',
    cert_status   TINYINT DEFAULT 0 COMMENT '0 未提交 / 1 待审 / 2 通过 / 3 驳回',
    channel_id    BIGINT DEFAULT NULL,
    vip_level     TINYINT DEFAULT 0 COMMENT 'v1 不使用',
    points        INT DEFAULT 0 COMMENT 'v1 不使用',
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_openid (openid)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='小程序用户';

CREATE TABLE IF NOT EXISTS ums_identity_cert (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id               BIGINT DEFAULT NULL,
    identity_type         TINYINT DEFAULT NULL COMMENT '1 团长 / 2 摊主',
    real_name             VARCHAR(50) DEFAULT NULL,
    id_card_no            VARCHAR(255) DEFAULT NULL COMMENT '加密存储',
    material_urls         TEXT,
    status                TINYINT DEFAULT 1 COMMENT '1 待审 / 2 通过 / 3 驳回',
    audit_user_id         BIGINT DEFAULT NULL,
    audit_time            DATETIME DEFAULT NULL,
    audit_remark          VARCHAR(255) DEFAULT NULL,
    proof_coupon_granted  TINYINT DEFAULT 0,
    create_time           DATETIME DEFAULT NULL,
    update_time           DATETIME DEFAULT NULL,
    deleted               TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='身份认证记录';

CREATE TABLE IF NOT EXISTS ums_address (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT DEFAULT NULL,
    receiver_name  VARCHAR(50) DEFAULT NULL,
    receiver_phone VARCHAR(20) DEFAULT NULL,
    province       VARCHAR(50) DEFAULT NULL,
    city           VARCHAR(50) DEFAULT NULL,
    district       VARCHAR(50) DEFAULT NULL,
    detail         VARCHAR(255) DEFAULT NULL,
    is_default     TINYINT DEFAULT 0,
    create_time    DATETIME DEFAULT NULL,
    update_time    DATETIME DEFAULT NULL,
    deleted        TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='收货地址';

CREATE TABLE IF NOT EXISTS sys_channel (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    channel_name VARCHAR(100) DEFAULT NULL,
    channel_code VARCHAR(50) DEFAULT NULL,
    status       TINYINT DEFAULT 1,
    create_time  DATETIME DEFAULT NULL,
    update_time  DATETIME DEFAULT NULL,
    deleted      TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_channel_code (channel_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='渠道';
