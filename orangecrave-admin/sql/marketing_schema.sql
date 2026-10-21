-- ============================================================
-- marketing 模块 DDL（01 篇 §7）
-- ============================================================

CREATE TABLE IF NOT EXISTS mkt_activity (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    activity_name VARCHAR(100) DEFAULT NULL,
    activity_type TINYINT DEFAULT NULL COMMENT '1 拼团 / 2 秒杀 / 3 优惠券 / 4 邀请好友',
    spu_ids       JSON,
    sku_ids       JSON,
    start_time    DATETIME DEFAULT NULL,
    end_time      DATETIME DEFAULT NULL,
    rule_config   JSON,
    status        TINYINT DEFAULT 1 COMMENT '0 停用 / 1 进行 / 2 结束',
    -- 04 篇 §3.3：秒杀走独立配额，不占 SKU 库存；扣减只由下单/取消驱动，rule_config 里的 stock 只是录入入口
    total_stock   INT DEFAULT NULL COMMENT '秒杀独立库存总量，NULL 表示不限量',
    sold_stock    INT NOT NULL DEFAULT 0 COMMENT '秒杀已扣减数量',
    create_time   DATETIME DEFAULT NULL,
    update_time   DATETIME DEFAULT NULL,
    deleted       TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='活动（拼团/秒杀/优惠券/邀请共用）';

CREATE TABLE IF NOT EXISTS mkt_group (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    activity_id    BIGINT DEFAULT NULL,
    leader_user_id BIGINT DEFAULT NULL,
    target_size    INT DEFAULT 3,
    current_size   INT DEFAULT 1,
    status         TINYINT DEFAULT 1 COMMENT '1 拼团中 / 2 已成团 / 3 拼团失败',
    expire_time    DATETIME DEFAULT NULL,
    is_simulated   TINYINT DEFAULT 0,
    create_time    DATETIME DEFAULT NULL,
    update_time    DATETIME DEFAULT NULL,
    deleted        TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status_expire (status, expire_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='拼团';

CREATE TABLE IF NOT EXISTS mkt_group_member (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    group_id   BIGINT NOT NULL,
    user_id    BIGINT DEFAULT NULL,
    order_id   BIGINT DEFAULT NULL,
    is_leader  TINYINT DEFAULT 0,
    is_virtual TINYINT DEFAULT 0,
    join_time  DATETIME DEFAULT NULL,
    status     TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted    TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_group (group_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='团员';

CREATE TABLE IF NOT EXISTS mkt_coupon (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    activity_id BIGINT DEFAULT NULL,
    coupon_name VARCHAR(100) DEFAULT NULL,
    coupon_type TINYINT DEFAULT 1 COMMENT '1 满减 / 2 折扣 / 3 无门槛',
    threshold   DECIMAL(12,2) DEFAULT 0.00,
    amount      DECIMAL(12,2) DEFAULT 0.00,
    discount    DECIMAL(10,2) DEFAULT NULL,
    valid_days  INT DEFAULT 30,
    total_qty   INT DEFAULT 0,
    issued_qty  INT DEFAULT 0,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='优惠券模板';

CREATE TABLE IF NOT EXISTS mkt_coupon_user (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    coupon_id   BIGINT NOT NULL,
    user_id     BIGINT DEFAULT NULL,
    code        VARCHAR(32) DEFAULT NULL,
    status      TINYINT DEFAULT 1 COMMENT '1 未使用 / 2 已使用 / 3 已过期',
    use_time    DATETIME DEFAULT NULL,
    order_id    BIGINT DEFAULT NULL,
    expire_time DATETIME DEFAULT NULL,
    source      TINYINT DEFAULT 1 COMMENT '1 人工发放 / 2 自动发放 / 3 领券中心',
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户持券';

CREATE TABLE IF NOT EXISTS mkt_invite (
    id                     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    activity_id            BIGINT DEFAULT NULL COMMENT '关联邀请活动（activity_type=4）',
    inviter_user_id        BIGINT DEFAULT NULL COMMENT '邀请人',
    invitee_user_id        BIGINT DEFAULT NULL COMMENT '被邀请人',
    order_id               BIGINT DEFAULT NULL COMMENT '被邀请人首次下单订单（生效归因）',
    status                 TINYINT DEFAULT 1 COMMENT '1 已记录 / 2 已生效 / 3 已失效',
    inviter_reward_granted TINYINT DEFAULT 0,
    invitee_reward_granted TINYINT DEFAULT 0,
    create_time            DATETIME DEFAULT NULL,
    update_time            DATETIME DEFAULT NULL,
    deleted                TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_inviter (inviter_user_id),
    KEY idx_activity (activity_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='邀请好友记录';
