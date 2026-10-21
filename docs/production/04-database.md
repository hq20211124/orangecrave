# 04 — 数据库高可用

> 目标：MySQL 主从切换 RTO < 30s；备份恢复 RPO < 5min；慢查询可发现、可治理。

## 1. 现状

- 单实例 MySQL 8
- 无备份策略
- 无慢查询监控
- 应用直连单点

## 2. 方案一：云厂商 RDS（推荐，最省事）

### 2.1 选什么规格

| 环境 | 规格 | 说明 |
|------|------|------|
| dev | 1核2G | 开发自测够用 |
| staging | 2核4G | 与生产同版本，规模缩小 |
| prod | 4核16G 起步 | 按压测结果调整 |

### 2.2 必须开启的功能

以阿里云 RDS 为例：

| 功能 | 配置 | 说明 |
|------|------|------|
| 高可用版 | 必选 | 主备双节点，自动切换 |
| 自动备份 | 每天全量 + 每 5 分钟 binlog | RPO < 5min |
| 备份保留 | 7 天 | 可回滚到 7 天内任意时间点 |
| 慢日志 | 开启，阈值 1s | 超过 1s 的 SQL 记录下来 |
| 性能洞察 | 开启 | 可视化分析慢查询 |
| SSL 加密 | 开启 | 传输加密 |
| 白名单 | 仅允许 K8s 节点 IP | 不允许公网直连 |

### 2.3 应用侧配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://rm-xxxx.mysql.rds.aliyuncs.com:3306/orangecrave?useSSL=true&requireSSL=true&serverTimezone=Asia/Shanghai&autoReconnect=true&failOverReadOnly=false
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000
      idle-timeout: 600000
      max-lifetime: 1800000     # 30 分钟，必须小于 MySQL wait_timeout（默认 28800s）
      connection-test-query: SELECT 1
      validation-timeout: 3000
```

**关键参数解释**：
- `max-lifetime: 1800000` — 连接最大存活 30 分钟。MySQL 主备切换时旧连接会失效，HikariCP 在 maxLifetime 后自动重建连接
- `autoReconnect=true` — JDBC 驱动级别自动重连
- `connection-test-query: SELECT 1` — 从池里取连接时先验证有效性

## 3. 方案二：自建 MySQL 主从（无云 RDS 时）

### 3.1 架构

```
                ┌─────────────┐
                │  MySQL      │
                │  Primary    │←── 写
                │  (主)       │
                └──────┬──────┘
                       │ binlog 复制
                ┌──────▼──────┐
                │  MySQL      │
                │  Replica    │←── 读（可选）
                │  (从)       │
                └─────────────┘
```

### 3.2 Docker Compose 主从配置

`docker-compose.yml` 追加：

```yaml
services:
  mysql-primary:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: orangecrave
    volumes:
      - mysql-primary-data:/var/lib/mysql
      - ./mysql/my-primary.cnf:/etc/mysql/conf.d/custom.cnf
    ports:
      - "3306:3306"
    command: --server-id=1 --log-bin=mysql-bin --binlog-format=ROW

  mysql-replica:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
    volumes:
      - mysql-replica-data:/var/lib/mysql
      - ./mysql/my-replica.cnf:/etc/mysql/conf.d/custom.cnf
    ports:
      - "3307:3306"
    command: --server-id=2 --relay-log=mysql-relay-bin
    depends_on:
      - mysql-primary

volumes:
  mysql-primary-data:
  mysql-replica-data:
```

### 3.3 配置主从复制

```bash
# 1. 在主库创建复制用户
docker exec -it mysql-primary mysql -uroot -p -e "
  CREATE USER 'repl'@'%' IDENTIFIED BY 'repl_password';
  GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
  FLUSH PRIVILEGES;
  SHOW MASTER STATUS;
"

# 2. 记录 File 和 Position，例如 mysql-bin.000003 / 857

# 3. 在从库配置复制
docker exec -it mysql-replica mysql -uroot -p -e "
  CHANGE MASTER TO
    MASTER_HOST='mysql-primary',
    MASTER_USER='repl',
    MASTER_PASSWORD='repl_password',
    MASTER_LOG_FILE='mysql-bin.000003',
    MASTER_LOG_POS=857;
  START SLAVE;
  SHOW SLAVE STATUS\G
"

# 4. 确认 Slave_IO_Running 和 Slave_SQL_Running 都是 Yes
```

### 3.4 应用侧读写分离

**注意：当前版本不急着做读写分离。** 单库 4 核 16G 对中小规模足够。只有当 QPS 超过单库承载能力时才需要。届时引入 ShardingSphere-JDBC 或 MyCat 做透明读写分离。

## 4. 备份与恢复

### 4.1 备份策略

| 类型 | 频率 | 保留 | 工具 |
|------|------|------|------|
| 全量备份 | 每天凌晨 2 点 | 7 天 | mysqldump / XtraBackup |
| Binlog 增量 | 实时 | 7 天 | MySQL binlog |
| 快照 | 每天 | 7 天 | 云厂商磁盘快照 |

### 4.2 全量备份脚本

```bash
#!/bin/bash
# backup.sh — 每日全量备份
set -euo pipefail

BACKUP_DIR="/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
DB_HOST="${DB_HOST:-localhost}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASSWORD:?必须设置 DB_PASSWORD}"
DB_NAME="orangecrave"

mkdir -p ${BACKUP_DIR}

# 使用 mysqldump 全量备份（单事务一致性，不锁表）
mysqldump \
  -h${DB_HOST} \
  -u${DB_USER} \
  -p${DB_PASS} \
  --single-transaction \
  --routines \
  --triggers \
  --all-databases \
  | gzip > ${BACKUP_DIR}/orangecrave_${DATE}.sql.gz

# 删除 7 天前的备份
find ${BACKUP_DIR} -name "orangecrave_*.sql.gz" -mtime +7 -delete

echo "备份完成: ${BACKUP_DIR}/orangecrave_${DATE}.sql.gz"
```

### 4.3 恢复演练（每季度做一次）

```bash
#!/bin/bash
# restore-test.sh — 备份恢复演练
set -euo pipefail

BACKUP_FILE=${1:?用法: ./restore-test.sh <备份文件>}

echo ">>> 创建临时库"
mysql -uroot -p -e "CREATE DATABASE orangecrave_restore_test;"

echo ">>> 恢复数据"
gunzip < ${BACKUP_FILE} | mysql -uroot -p orangecrave_restore_test

echo ">>> 验证数据"
TABLE_COUNT=$(mysql -uroot -p -N -e "
  SELECT COUNT(*) FROM information_schema.tables 
  WHERE table_schema='orangecrave_restore_test';
")
echo "恢复了 ${TABLE_COUNT} 张表"

echo ">>> 抽查订单数据"
mysql -uroot -p -e "
  SELECT COUNT(*) AS order_count, 
         MAX(create_time) AS latest_order 
  FROM orangecrave_restore_test.oms_order;
"

echo ">>> 清理"
mysql -uroot -p -e "DROP DATABASE orangecrave_restore_test;"
echo "恢复演练完成，RTO 计时结束"
```

## 5. 慢查询治理

### 5.1 开启慢查询日志

在 MySQL 配置中：

```ini
[mysqld]
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 1          # 超过 1 秒记录
log_queries_not_using_indexes = 1   # 没走索引的也记录
```

### 5.2 分析慢查询

```bash
# 用 pt-query-digest 分析慢查询日志
pt-query-digest /var/log/mysql/slow.log | head -100

# 或直接用 mysqldumpslow（MySQL 自带）
mysqldumpslow -s t -t 10 /var/log/mysql/slow.log
```

### 5.3 常见慢查询模式及优化

| 模式 | 原因 | 优化 |
|------|------|------|
| `SELECT *` | 查了不需要的列 | 只查需要的列 |
| `WHERE` 无索引 | 全表扫描 | 加合适索引 |
| `ORDER BY create_time DESC LIMIT 20` | 大偏移量 | 加索引 + 游标分页 |
| `JOIN` 多表 | 关联字段无索引 | 关联字段加索引 |
| `COUNT(*)` 大表 | 全表统计 | 用近似值或汇总表 |

### 5.4 当前系统需要关注的查询

根据业务特点，以下查询最容易出性能问题：

| 查询 | 表 | 建议索引 |
|------|----|----------|
| 订单列表（按状态+时间） | `oms_order` | `idx_status_create_time(status, create_time)` |
| 订单列表（按用户+时间） | `oms_order` | `idx_user_id_create_time(user_id, create_time)` |
| 秒杀活动查询 | `mkt_seckill` | `idx_status_start_time(status, start_time)` |
| 作品列表（按用户） | `wms_work` | `idx_user_id(user_id)` |
| 操作日志（按时间） | `sys_oper_log` | `idx_create_time(create_time)` |

**这些索引在 `sql/*_schema.sql` 中已部分存在。部署后观察慢日志，按需补充。**

## 6. 容量规划

### 6.1 数据量估算

| 表 | 日均增长 | 年增长 | 单条大小 | 年存储 |
|----|----------|--------|----------|--------|
| `oms_order` | 1,000 | 36.5 万 | 2KB | 730MB |
| `oms_order_item` | 3,000 | 110 万 | 1KB | 1.1GB |
| `sys_oper_log` | 5,000 | 183 万 | 0.5KB | 915MB |
| `sys_login_log` | 10,000 | 365 万 | 0.3KB | 1.1GB |

### 6.2 清理策略

| 表 | 保留时长 | 清理方式 |
|----|----------|----------|
| `sys_oper_log` | 6 个月 | 定时任务归档到冷存储 |
| `sys_login_log` | 3 个月 | 定时任务删除 |
| `sys_idempotency` | 24 小时 | 已有 `IdempotencyCleanupTask` |

## 7. 检查清单

- [ ] MySQL 主备/集群部署完成，主从延迟 < 1s
- [ ] 每日全量备份 + binlog 增量备份正常运行
- [ ] 恢复演练通过，RTO < 30min，RPO < 5min
- [ ] 慢查询日志开启，阈值 1s
- [ ] 核心查询有索引（EXPLAIN 验证）
- [ ] 连接池参数与 MySQL 配置匹配
- [ ] 白名单限制，仅允许应用服务器 IP
- [ ] SSL 加密传输
