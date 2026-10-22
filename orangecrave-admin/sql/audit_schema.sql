SET NAMES utf8mb4;
-- ============================================================
-- audit 模块 DDL（01 篇 §6）
-- ============================================================

CREATE TABLE IF NOT EXISTS aud_image_audit (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    biz_type      TINYINT NOT NULL DEFAULT 1 COMMENT '1 定制图 / 2 作品 / 3 头像',
    biz_id        BIGINT DEFAULT NULL,
    image_url     VARCHAR(500) DEFAULT NULL,
    pHash         BIGINT DEFAULT NULL,
    auto_result   TINYINT DEFAULT NULL COMMENT '1 通过 / 2 疑似 / 3 违规',
    auto_remark   VARCHAR(255) DEFAULT NULL,
    manual_result TINYINT DEFAULT NULL COMMENT '1 通过 / 2 驳回',
    audit_user_id BIGINT DEFAULT NULL,
    audit_time    DATETIME DEFAULT NULL,
    audit_remark  VARCHAR(255) DEFAULT NULL,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_auto_result (auto_result)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='图片审核记录';

CREATE TABLE IF NOT EXISTS aud_blacklist (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    pHash       BIGINT DEFAULT NULL,
    source      VARCHAR(50) DEFAULT NULL,
    remark      VARCHAR(255) DEFAULT NULL,
    status      TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_pHash (pHash)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='违规图库';

CREATE TABLE IF NOT EXISTS aud_sensitive_word (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    word        VARCHAR(100) DEFAULT NULL,
    category    VARCHAR(50) DEFAULT NULL,
    action      VARCHAR(20) DEFAULT NULL COMMENT '替换 / 拒绝',
    status      TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='敏感词';

CREATE TABLE IF NOT EXISTS aud_copyright_promise (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT DEFAULT NULL,
    biz_type        TINYINT DEFAULT NULL,
    biz_id          BIGINT DEFAULT NULL,
    promise_version VARCHAR(20) DEFAULT NULL,
    ip              VARCHAR(50) DEFAULT NULL,
    create_time     DATETIME DEFAULT NULL,
    update_time     DATETIME DEFAULT NULL,
    deleted         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='版权承诺';
