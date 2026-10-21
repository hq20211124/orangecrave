-- ============================================================
-- work 模块 DDL（01 篇 §4）
-- ============================================================

CREATE TABLE IF NOT EXISTS wms_work (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
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
    deleted        TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='作品';

CREATE TABLE IF NOT EXISTS wms_work_image (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    work_id    BIGINT NOT NULL,
    slot_index INT DEFAULT 0,
    image_url  VARCHAR(500) DEFAULT NULL,
    thumb_url  VARCHAR(500) DEFAULT NULL,
    copies     INT DEFAULT 1,
    pHash      BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_work (work_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='作品图槽位';

CREATE TABLE IF NOT EXISTS wms_collection (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT DEFAULT NULL,
    collection_name VARCHAR(100) DEFAULT NULL,
    cover_url       VARCHAR(255) DEFAULT NULL,
    status          TINYINT DEFAULT NULL,
    create_time     DATETIME DEFAULT NULL,
    update_time     DATETIME DEFAULT NULL,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='作品合集';

CREATE TABLE IF NOT EXISTS wms_collection_item (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    collection_id BIGINT NOT NULL,
    work_id       BIGINT NOT NULL,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_collection (collection_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='合集作品';

CREATE TABLE IF NOT EXISTS wms_share_code (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    share_code      VARCHAR(32) NOT NULL,
    user_id         BIGINT DEFAULT NULL,
    work_id         BIGINT DEFAULT NULL,
    collection_id   BIGINT DEFAULT NULL,
    share_type      TINYINT DEFAULT 1 COMMENT '1 普通 / 2 无料 / 3 合集',
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
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_share_code (share_code),
    KEY idx_status_expire (status, expire_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='分享码';
