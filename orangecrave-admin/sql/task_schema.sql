SET NAMES utf8mb4;
-- ============================================================
-- task 模块 DDL（01 篇 §10）
-- ============================================================

CREATE TABLE IF NOT EXISTS tsk_delay_log (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    task_type   VARCHAR(50) NOT NULL,
    biz_id      BIGINT NOT NULL,
    fire_time   DATETIME DEFAULT NULL,
    exec_time   DATETIME DEFAULT NULL,
    status      TINYINT DEFAULT 1 COMMENT '1 待执行 / 2 成功 / 3 失败',
    retry_count INT DEFAULT 0,
    error_msg   TEXT,
    create_time DATETIME DEFAULT NULL,
    update_time DATETIME DEFAULT NULL,
    deleted     TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_biz (task_type, biz_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='延迟任务日志';
