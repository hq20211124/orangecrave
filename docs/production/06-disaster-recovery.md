# 06 — 容灾与高可用

> 目标：单可用区宕机，30 秒内自动切换；数据丢失 < 5 分钟。

## 1. 容灾等级

| 等级 | 名称 | 恢复时间目标 RTO | 数据恢复点 RPO | 成本 |
|------|------|------------------|----------------|------|
| L1 | 备份恢复 | 数小时 | 24 小时 | 最低 |
| L2 | 主备切换 | 5~30 分钟 | 5 分钟 | 低 |
| L3 | 同城双活 | 30 秒 | 接近 0 | 中 |
| L4 | 异地多活 | 30 秒 | 接近 0 | 高 |

**推荐目标：L2 起步，L3 为生产目标。** L4 对大多数企业过度设计。

## 2. 当前系统的单点风险

| 组件 | 现状 | 风险 | 改进 |
|------|------|------|------|
| 应用 | 单 Pod | 挂了就全停 | K8s 多副本 + 反亲和调度 |
| MySQL | 单实例 | 数据丢失 + 服务中断 | 主备 / RDS 高可用 |
| Redis | 单实例（可选） | 缓存丢失，降级到 DB | Redis Sentinel / Cluster |
| 存储 | 本地磁盘 | 文件丢失 | S3（多副本） |
| 消息队列 | 内嵌 / Redis Stream | 消息丢失 | Redis Stream 持久化 + 消费者组 |

## 3. 应用层高可用

### 3.1 多副本 + 反亲和

在 `deployment.yaml` 中已配置 `replicas: 2`。进一步确保两个 Pod 不在同一节点：

```yaml
spec:
  template:
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchLabels:
                    app: orangecrave-admin
                topologyKey: kubernetes.io/hostname    # 尽量不在同一主机
```

### 3.2 跨可用区调度

如果 K8s 集群跨多个可用区（Zone）：

```yaml
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchLabels:
                  app: orangecrave-admin
              topologyKey: topology.kubernetes.io/zone    # 强制不在同一可用区
```

### 3.3 优雅停机

K8s 滚动更新或节点维护时，确保不丢请求：

```
K8s 删除 Pod 的流程：
1. 从 Service Endpoints 中摘除（不再接新流量）
2. 执行 preStop Hook（sleep 15s，等待负载均衡器完全摘除）
3. 发送 SIGTERM 给应用进程
4. Spring Boot 收到信号，停止接受新请求，等待进行中请求完成
5. 超时后（30s）强制 SIGKILL
```

应用侧已配置：
- `server.shutdown: graceful` — Spring Boot 优雅停机
- `spring.lifecycle.timeout-per-shutdown-phase: 30s` — 等待最长 30 秒

## 4. 数据库高可用

### 4.1 云 RDS 高可用（推荐）

云 RDS 高可用版自带：
- 主备双节点，自动同步
- 主节点故障，30 秒内自动切换到备节点
- 切换后 VIP 不变，应用无感知

**应用侧需要做的**：连接池配置 `max-lifetime` 小于切换时间（已在部署文档中配置 30 分钟）。

### 4.2 自建主备切换

如果用自建 MySQL，需要一个切换工具：

| 工具 | 说明 |
|------|------|
| **MHA** | 经典 MySQL 高可用方案，自动检测主库故障并切换 |
| **Orchestrator** | GitHub 开源，Web UI 管理拓扑，支持自动切换 |
| **MySQL Router** | 官方方案，配合 InnoDB Cluster |

**推荐 Orchestrator**，部署简单：

```bash
# 安装 Orchestrator
docker run -d --name orchestrator \
  -p 3000:3000 \
  -e ORCHESTRATOR_PASSWORD=admin \
  openarkcode/orchestrator:latest

# 注册 MySQL 主从
curl -X POST http://localhost:3000/api/discover/mysql-primary:3306
curl -X POST http://localhost:3000/api/discover/mysql-replica:3306
```

## 5. Redis 高可用

### 5.1 方案一：云 Redis（推荐）

云厂商 Redis 默认主备 + 自动切换，应用无感知。连接串使用云厂商提供的 VIP。

### 5.2 方案二：Redis Sentinel

```yaml
# docker-compose 追加
services:
  redis-master:
    image: redis:7
    ports:
      - "6379:6379"
    command: redis-server --requirepass ${REDIS_PASSWORD}

  redis-replica:
    image: redis:7
    command: redis-server --replicaof redis-master 6379 --requirepass ${REDIS_PASSWORD} --masterauth ${REDIS_PASSWORD}
    depends_on:
      - redis-master

  redis-sentinel-1:
    image: redis:7
    command: redis-sentinel /etc/sentinel.conf
    volumes:
      - ./redis/sentinel1.conf:/etc/sentinel.conf
    depends_on:
      - redis-master

  redis-sentinel-2:
    image: redis:7
    command: redis-sentinel /etc/sentinel.conf
    volumes:
      - ./redis/sentinel2.conf:/etc/sentinel.conf
    depends_on:
      - redis-master

  redis-sentinel-3:
    image: redis:7
    command: redis-sentinel /etc/sentinel.conf
    volumes:
      - ./redis/sentinel3.conf:/etc/sentinel.conf
    depends_on:
      - redis-master
```

应用侧配置改为 Sentinel 模式：

```yaml
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes: sentinel-1:26379,sentinel-2:26379,sentinel-3:26379
      password: ${REDIS_PASSWORD}
```

## 6. 存储高可用

S3 天然多副本，无需额外配置。但需要确认：

- [ ] S3 Bucket 跨区域复制已开启（如果需要异地容灾）
- [ ] 文件上传失败有重试机制
- [ ] 本地存储模式有定期备份到 S3 的任务

## 7. 消息队列高可用

### 7.1 Redis Stream 持久化

Redis Stream 的消息默认在内存中，需要确认 Redis 配置了 AOF 持久化：

```conf
# redis.conf
appendonly yes
appendfsync everysec    # 每秒刷盘，性能与可靠性平衡
```

### 7.2 消费者组确认

Redis Stream 的消费者组保证消息至少被消费一次。消费者重启后，未确认的消息（pending）可以重新消费。

**当前系统已实现**：消费者组 + trim 防膨胀 + 失败 ack 不重投。需要确认：
- [ ] 消费者异常重启后，pending 消息能被重新消费
- [ ] 消息处理是幂等的（重复消费不产生脏数据）

## 8. 灾难恢复预案

### 8.1 场景一：应用 Pod 全部挂掉

**触发条件**：K8s 节点故障 / 应用 bug 导致 OOM

**恢复步骤**：
1. K8s 自动重启 Pod（restartPolicy: Always）
2. 如果反复 CrashLoopBackOff，回滚到上一个版本：`kubectl rollout undo deployment/orangecrave-admin -n orangecrave`
3. 检查日志定位问题：`kubectl logs -f deployment/orangecrave-admin -n orangecrave --previous`

**RTO**：< 2 分钟（自动）/ < 5 分钟（手动回滚）

### 8.2 场景二：MySQL 主库挂掉

**触发条件**：硬件故障 / 磁盘满 / 网络分区

**恢复步骤**（云 RDS）：
1. RDS 自动切换到备节点（< 30 秒）
2. 应用连接池自动重建连接（max-lifetime 30 分钟内）
3. 验证：`curl https://admin.orangecrave.example.com/admin-api/health`

**恢复步骤**（自建）：
1. Orchestrator 检测到主库不可达
2. 自动提升从库为主库
3. 更新 DNS / VIP 指向新主库
4. 应用重连

**RTO**：< 30 秒（RDS）/ < 5 分钟（自建）

### 8.3 场景三：整个可用区宕机

**触发条件**：机房断电 / 网络中断 / 自然灾害

**前提**：应用和数据库都跨可用区部署

**恢复步骤**：
1. 负载均衡器自动将流量路由到健康可用区
2. K8s 在健康可用区启动新 Pod
3. 数据库如果使用 RDS 多可用区版，自动切换
4. 验证所有服务恢复

**RTO**：< 5 分钟

### 8.4 场景四：数据损坏/误删

**触发条件**：人为误操作 / 程序 bug

**恢复步骤**：
1. 停止写入（限流 / 下线服务）
2. 从最近的备份恢复：`./restore-test.sh /backup/mysql/orangecrave_20261020_020000.sql.gz`
3. 用 binlog 回放到误删前一刻：`mysqlbinlog --start-datetime="2026-10-20 00:00:00" --stop-datetime="2026-10-20 01:30:00" mysql-bin.000xxx | mysql -uroot -p`
4. 验证数据完整性
5. 恢复服务

**RTO**：30 分钟 ~ 2 小时（取决于数据量）
**RPO**：< 5 分钟（binlog 间隔）

## 9. 容灾演练计划

| 演练 | 频率 | 内容 | 验证标准 |
|------|------|------|----------|
| 应用 Pod 故障 | 每月 | `kubectl delete pod` 随机删一个 | 服务不中断，自动恢复 |
| MySQL 切换 | 每季度 | 手动触发主备切换 | RTO < 30s，无数据丢失 |
| 备份恢复 | 每季度 | 恢复到临时库，抽查数据 | 数据完整，RTO < 30min |
| 全链路容灾 | 每年 | 模拟可用区宕机 | 5 分钟内恢复，用户无感知 |

## 10. 检查清单

- [ ] 应用至少 2 副本，跨节点调度
- [ ] 数据库主备/高可用已配置，切换 RTO < 30s
- [ ] 备份策略已执行，恢复演练通过
- [ ] Redis 高可用已配置（如果使用）
- [ ] 优雅停机已配置，滚动更新不丢请求
- [ ] 灾难恢复预案文档已评审
- [ ] 至少做过一次容灾演练
