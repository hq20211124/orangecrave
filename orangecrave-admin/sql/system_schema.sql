-- ============================================================
-- 橙刻定制 · 后台管理系统  数据库 DDL —— 系统管理（01 篇 §1）
-- MySQL 8.0 / utf8mb4 / InnoDB
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL COMMENT '登录名',
    password        VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
    nickname        VARCHAR(50)  DEFAULT NULL,
    phone           VARCHAR(20)  DEFAULT NULL,
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '0 停用 / 1 正常',
    last_login_time DATETIME     DEFAULT NULL,
    last_login_ip   VARCHAR(50)  DEFAULT NULL,
    create_time     DATETIME     DEFAULT NULL,
    update_time     DATETIME     DEFAULT NULL,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='管理员';

CREATE TABLE IF NOT EXISTS sys_role (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_name  VARCHAR(50)  DEFAULT NULL,
    role_key   VARCHAR(50)  DEFAULT NULL COMMENT '唯一标识',
    remark     VARCHAR(255) DEFAULT NULL,
    status     TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME    DEFAULT NULL,
    update_time DATETIME    DEFAULT NULL,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色';

CREATE TABLE IF NOT EXISTS sys_menu (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    parent_id  BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单，0 为根',
    menu_name  VARCHAR(50)  NOT NULL,
    menu_type  TINYINT      NOT NULL COMMENT '1 目录 / 2 菜单 / 3 按钮',
    path       VARCHAR(200) DEFAULT NULL,
    perm_key   VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
    icon       VARCHAR(100) DEFAULT NULL,
    sort       INT          NOT NULL DEFAULT 0,
    create_time DATETIME    DEFAULT NULL,
    update_time DATETIME    DEFAULT NULL,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_parent (parent_id),
    KEY idx_perm (perm_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='菜单与按钮权限';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT  NOT NULL,
    role_id    BIGINT  NOT NULL,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_role (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户-角色';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_id    BIGINT  NOT NULL,
    menu_id    BIGINT  NOT NULL,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_role (role_id),
    KEY idx_menu (menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色-菜单';

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    oper_module   VARCHAR(50)  DEFAULT NULL,
    oper_type     VARCHAR(20)  DEFAULT NULL,
    oper_desc     VARCHAR(255) DEFAULT NULL,
    method        VARCHAR(200) DEFAULT NULL,
    request_url   VARCHAR(255) DEFAULT NULL,
    request_param TEXT,
    before_data   JSON,
    after_data    JSON,
    oper_user_id  BIGINT       DEFAULT NULL,
    oper_user_name VARCHAR(50) DEFAULT NULL,
    oper_ip       VARCHAR(50)  DEFAULT NULL,
    oper_time     DATETIME     DEFAULT NULL,
    status        TINYINT      DEFAULT 1 COMMENT '0 失败 / 1 成功',
    error_msg     TEXT,
    create_time   DATETIME     DEFAULT NULL,
    update_time   DATETIME     DEFAULT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_oper_time (oper_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志';

CREATE TABLE IF NOT EXISTS sys_login_log (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_name  VARCHAR(50) DEFAULT NULL,
    ip         VARCHAR(50) DEFAULT NULL,
    browser    VARCHAR(255) DEFAULT NULL,
    os         VARCHAR(100) DEFAULT NULL,
    status     TINYINT     DEFAULT 1,
    msg        VARCHAR(255) DEFAULT NULL,
    login_time DATETIME    DEFAULT NULL,
    create_time DATETIME   DEFAULT NULL,
    update_time DATETIME   DEFAULT NULL,
    deleted    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_login_time (login_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='登录日志';

CREATE TABLE IF NOT EXISTS sys_config (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    config_key   VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_type  TINYINT      NOT NULL DEFAULT 1 COMMENT '0 内置 / 1 可改',
    remark       VARCHAR(255) DEFAULT NULL,
    create_time  DATETIME     DEFAULT NULL,
    update_time  DATETIME     DEFAULT NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统参数配置';

CREATE TABLE IF NOT EXISTS sys_idempotency (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    idem_key      VARCHAR(64)  NOT NULL COMMENT '客户端 Idempotency-Key 头',
    api           VARCHAR(200) NOT NULL COMMENT 'METHOD + URI',
    request_hash  VARCHAR(64)  DEFAULT NULL COMMENT '首次请求体 sha256，用于业务唯一键二次比对',
    response_body MEDIUMTEXT   COMMENT '首次成功响应，重复请求直接重放',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1 处理中 / 2 已完成 / 3 失败可重试',
    create_time   DATETIME     DEFAULT NULL,
    update_time   DATETIME     DEFAULT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_key_api (idem_key, api),
    KEY idx_create_time (create_time) COMMENT '幂等记录 TTL 清理按时间扫描'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='写操作幂等去重（02 篇 §1.5）';
