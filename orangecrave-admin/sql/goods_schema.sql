-- ============================================================
-- goods 模块 DDL（01 篇 §3）
-- ============================================================

CREATE TABLE IF NOT EXISTS pms_series (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    series_name VARCHAR(100) NOT NULL,
    cover       VARCHAR(255) DEFAULT NULL,
    ip_name     VARCHAR(100) DEFAULT NULL,
    sort        INT      DEFAULT 0,
    status      TINYINT  DEFAULT 1,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系列/IP主题';

CREATE TABLE IF NOT EXISTS pms_category (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    parent_id     BIGINT  DEFAULT 0,
    sort          INT     DEFAULT 0,
    status        TINYINT DEFAULT 1,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='品类';

CREATE TABLE IF NOT EXISTS pms_spu (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    spu_name            VARCHAR(200) NOT NULL,
    sub_title           VARCHAR(255) DEFAULT NULL,
    cover               VARCHAR(255) DEFAULT NULL,
    images              TEXT,
    series_id           BIGINT DEFAULT NULL,
    category_id         BIGINT DEFAULT NULL,
    goods_type          TINYINT NOT NULL DEFAULT 1 COMMENT '1 定制 / 2 现货',
    pricing_type        TINYINT NOT NULL DEFAULT 1 COMMENT '1 按张 / 2 按面积 / 3 数量阶梯',
    base_price          DECIMAL(12,2) DEFAULT 0.00,
    area_price          DECIMAL(12,2) DEFAULT 0.00,
    pricing_config      TEXT COMMENT '阶梯配置 JSON',
    designer_id         BIGINT DEFAULT NULL,
    min_order_qty       INT DEFAULT 1,
    increment_qty       INT DEFAULT 1,
    freight_template_id BIGINT DEFAULT NULL,
    need_proof          TINYINT DEFAULT 0,
    status              TINYINT NOT NULL DEFAULT 1,
    create_time         DATETIME DEFAULT NULL,
    update_time         DATETIME DEFAULT NULL,
    deleted             TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_series (series_id),
    KEY idx_category (category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商品 SPU';

CREATE TABLE IF NOT EXISTS pms_spec_group (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    spu_id      BIGINT NOT NULL,
    group_name  VARCHAR(50) DEFAULT NULL,
    sort        INT DEFAULT 0,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_spu (spu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='规格组';

CREATE TABLE IF NOT EXISTS pms_spec_value (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    group_id   BIGINT NOT NULL,
    value_name VARCHAR(50) DEFAULT NULL,
    sort       INT DEFAULT 0,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_group (group_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='规格值';

CREATE TABLE IF NOT EXISTS pms_sku (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    spu_id         BIGINT NOT NULL,
    sku_code       VARCHAR(64) NOT NULL,
    spec_value_ids TEXT,
    price          DECIMAL(12,2) DEFAULT 0.00,
    stock          INT DEFAULT 0,
    weight         DECIMAL(10,2) DEFAULT 0.00,
    image          VARCHAR(255) DEFAULT NULL,
    length_mm      INT DEFAULT NULL,
    width_mm       INT DEFAULT NULL,
    status         TINYINT DEFAULT 1,
    create_time    DATETIME DEFAULT NULL,
    update_time    DATETIME DEFAULT NULL,
    deleted        TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_spu (spu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='SKU 规格组合';

CREATE TABLE IF NOT EXISTS pms_craft (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    craft_name     VARCHAR(50) NOT NULL,
    craft_type     VARCHAR(50) DEFAULT NULL,
    add_price      DECIMAL(12,2) DEFAULT 0.00,
    add_price_type TINYINT NOT NULL DEFAULT 1 COMMENT '1 固定 / 2 按面积',
    status         TINYINT DEFAULT 1,
    create_time    DATETIME DEFAULT NULL,
    update_time    DATETIME DEFAULT NULL,
    deleted        TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='工艺';

CREATE TABLE IF NOT EXISTS pms_sku_craft (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sku_id      BIGINT NOT NULL,
    craft_id    BIGINT NOT NULL,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_sku (sku_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='SKU-工艺';

CREATE TABLE IF NOT EXISTS pms_freight_template (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    template_name VARCHAR(100) NOT NULL,
    charge_type   TINYINT NOT NULL DEFAULT 1 COMMENT '1 按件 / 2 按重 / 3 按地区',
    first_unit    INT DEFAULT 1,
    first_fee     DECIMAL(12,2) DEFAULT 0.00,
    extra_unit    INT DEFAULT 1,
    extra_fee     DECIMAL(12,2) DEFAULT 0.00,
    free_threshold DECIMAL(12,2) DEFAULT NULL,
    region_rules  TEXT,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='运费模板';

CREATE TABLE IF NOT EXISTS pms_designer (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    designer_name VARCHAR(100) NOT NULL,
    avatar        VARCHAR(255) DEFAULT NULL,
    intro         VARCHAR(255) DEFAULT NULL,
    status        TINYINT DEFAULT 1,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='画师';

CREATE TABLE IF NOT EXISTS pms_template (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    template_name VARCHAR(100) NOT NULL,
    designer_id   BIGINT DEFAULT NULL,
    category_id   BIGINT DEFAULT NULL,
    preview_url   VARCHAR(255) DEFAULT NULL,
    source_url    VARCHAR(255) DEFAULT NULL,
    status        TINYINT DEFAULT 1,
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='画师模板库';
