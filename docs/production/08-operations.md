# 08 — 运维体系：告警、On-Call、SLO、CI/CD

> 目标：出了问题 5 分钟内有人知道；知道系统「应该多好」；代码从提交到上线全自动化。

## 1. SLO（服务等级目标）

### 1.1 什么是 SLO

SLO 是系统应该达到的服务水平目标。没有 SLO，运维就是「感觉还行」或「感觉不行」，没有量化标准。

### 1.2 OrangeCrave 的 SLO 定义

| 指标 | SLO | 说明 |
|------|-----|------|
| **可用性** | 99.9% | 每月允许宕机 43.2 分钟 |
| **P99 延迟** | < 500ms | 99% 的请求在 500ms 内完成 |
| **错误率** | < 0.1% | 每 1000 个请求允许 1 个 5xx 错误 |
| **数据持久性** | 99.999999% | 数据丢失概率极低（依赖 RDS + S3） |
| **RTO** | < 30 分钟 | 故障恢复时间目标 |
| **RPO** | < 5 分钟 | 数据丢失容忍度 |

### 1.3 怎么算可用性

```
可用性 = 正常服务时间 / 总时间 × 100%

例：一个月 30 天 = 43,200 分钟
99.9% 可用性 = 允许 43.2 分钟不可用
99.95% 可用性 = 允许 21.6 分钟不可用
99.99% 可用性 = 允许 4.32 分钟不可用
```

### 1.4 错误预算

```
错误预算 = (1 - SLO) × 总请求数

例：SLO 99.9%，月请求量 1000 万
错误预算 = 0.1% × 1000万 = 10,000 个错误请求

意义：本月可以容忍 1 万个错误。如果月初就用了 80% 预算，
说明系统不稳定，本月剩余时间禁止发布新功能。
```

## 2. 告警体系

### 2.1 告警分级

| 级别 | 响应时间 | 通知方式 | 例子 |
|------|----------|----------|------|
| P0 紧急 | 5 分钟 | 电话 + 短信 + 钉钉/飞书 | 服务完全不可用 |
| P1 严重 | 15 分钟 | 钉钉/飞书 + 邮件 | 错误率 > 5% |
| P2 警告 | 1 小时 | 钉钉/飞书 | P99 延迟 > 1s |
| P3 提醒 | 次日 | 邮件 | 磁盘使用 > 80% |

### 2.2 告警规则

在 [02-observability](02-observability.md) 的 Alertmanager 规则基础上，补充业务告警：

```yaml
groups:
  - name: orangecrave-business
    rules:
      # P0：服务不可用
      - alert: ServiceDown
        expr: up{application="orangecrave-admin"} == 0
        for: 1m
        labels:
          severity: P0
        annotations:
          summary: "OrangeCrave 服务完全不可用"

      # P0：数据库不可达
      - alert: DatabaseDown
        expr: mysql_up{job="mysql"} == 0
        for: 1m
        labels:
          severity: P0
        annotations:
          summary: "MySQL 数据库不可达"

      # P1：错误率过高
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 2m
        labels:
          severity: P1
        annotations:
          summary: "错误率超过 5%，当前 {{ $value | humanizePercentage }}"

      # P1：订单支付失败率过高
      - alert: HighPaymentFailureRate
        expr: rate(orangecrave_payment_total{result="fail"}[5m]) / rate(orangecrave_payment_total[5m]) > 0.1
        for: 5m
        labels:
          severity: P1
        annotations:
          summary: "支付失败率超过 10%"

      # P2：P99 延迟过高
      - alert: HighLatency
        expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m])) > 1.0
        for: 5m
        labels:
          severity: P2
        annotations:
          summary: "P99 延迟超过 1s，当前 {{ $value }}s"

      # P2：数据库连接池接近耗尽
      - alert: ConnectionPoolHigh
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
        for: 5m
        labels:
          severity: P2
        annotations:
          summary: "数据库连接池使用率超过 80%"

      # P3：磁盘使用率
      - alert: DiskUsageHigh
        expr: (node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes > 0.8
        for: 10m
        labels:
          severity: P3
        annotations:
          summary: "磁盘使用率超过 80%"
```

### 2.3 告警通知渠道

配置 Alertmanager 通知到钉钉/飞书：

```yaml
# alertmanager.yml
route:
  receiver: dingtalk
  routes:
    - match:
        severity: P0
      receiver: dingtalk-p0
      continue: true    # P0 同时发多个渠道

receivers:
  - name: dingtalk
    webhook_configs:
      - url: https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN
        send_resolved: true

  - name: dingtalk-p0
    webhook_configs:
      - url: https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN
        send_resolved: false
    email_configs:
      - to: oncall@orangecrave.example.com
        send_resolved: false
```

## 3. On-Call 手册

### 3.1 值班制度

| 班次 | 时间 | 人员 |
|------|------|------|
| 白班 | 09:00 - 21:00 | 主值班工程师 |
| 夜班 | 21:00 - 09:00 | 备值班工程师（仅 P0/P1 呼叫） |
| 周末 | 全天 | 轮换 |

### 3.2 常见故障处理手册

**故障 1：服务完全不可用（P0）**

```
1. 确认：curl https://admin.orangecrave.example.com/admin-api/health
2. 检查 Pod 状态：kubectl get pods -n orangecrave
3. 如果全部 CrashLoopBackOff：
   kubectl logs -f deployment/orangecrave-admin -n orangecrave --previous
4. 常见原因：
   - 数据库连接失败 → 检查 RDS 状态
   - OOM → kubectl describe pod 看内存限制
   - 配置错误 → 检查 ConfigMap/Secret 是否更新
5. 快速恢复：kubectl rollout undo deployment/orangecrave-admin -n orangecrave
6. 通知团队
```

**故障 2：数据库连接池耗尽（P1）**

```
1. 确认：Grafana 看 hikaricp_connections_pending > 0
2. 检查慢查询：RDS 控制台 → 慢日志
3. 临时扩容连接池（不改代码，改环境变量）：
   kubectl set env deployment/orangecrave-admin DB_POOL_SIZE=30 -n orangecrave
4. 长期方案：优化慢查询 / 加索引 / 加缓存
```

**故障 3：秒杀接口大量 429（预期行为）**

```
1. 确认：这是限流生效，不是 bug
2. 检查限流规则是否合理：orangecrave.rate-limit.rules
3. 如果业务需要更高限额：调整配置，不需要改代码
4. 如果攻击导致：在 WAF 层封 IP
```

**故障 4：支付回调延迟（P2）**

```
1. 检查微信支付回调是否到达：查 nginx 日志
2. 检查回调处理是否报错：查应用日志
3. 手动触发对账：确认是否有未入账订单
4. 如果大量未入账：联系微信支付客服
```

### 3.3 升级流程

```
P0 故障 15 分钟未解决 → 升级到技术负责人
P0 故障 30 分钟未解决 → 升级到 CTO
P1 故障 1 小时未解决 → 升级到技术负责人
```

## 4. CI/CD 流水线

### 4.1 流水线设计

```
代码提交 → 单元测试 → 构建镜像 → 推送到仓库 → 部署到 staging → 自动化测试 → 人工审批 → 部署到生产
```

### 4.2 GitHub Actions 示例

创建 `.github/workflows/ci-cd.yml`：

```yaml
name: CI/CD

on:
  push:
    branches: [master]
  pull_request:
    branches: [master]

env:
  REGISTRY: registry.example.com
  IMAGE: orangecrave/admin

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: maven

      - name: Run tests
        run: |
          cd orangecrave-admin
          mvn test -B

      - name: Upload test report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-report
          path: orangecrave-admin/**/target/surefire-reports/

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/master'
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ secrets.REGISTRY_USERNAME }}
          password: ${{ secrets.REGISTRY_PASSWORD }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE }}:latest
            ${{ env.REGISTRY }}/${{ env.IMAGE }}:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - name: Deploy to staging
        uses: azure/k8s-set-context@v4
        with:
          method: kubeconfig
          kubeconfig: ${{ secrets.KUBECONFIG_STAGING }}

      - name: Update image
        run: |
          kubectl -n orangecrave-staging set image deployment/orangecrave-admin \
            app=${{ env.REGISTRY }}/${{ env.IMAGE }}:${{ github.sha }}
          kubectl -n orangecrave-staging rollout status deployment/orangecrave-admin --timeout=300s

      - name: Smoke test
        run: |
          sleep 30
          curl -sf https://staging.orangecrave.example.com/admin-api/health || exit 1

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production    # 需要人工审批
    steps:
      - name: Deploy to production
        uses: azure/k8s-set-context@v4
        with:
          method: kubeconfig
          kubeconfig: ${{ secrets.KUBECONFIG_PROD }}

      - name: Update image
        run: |
          kubectl -n orangecrave set image deployment/orangecrave-admin \
            app=${{ env.REGISTRY }}/${{ env.IMAGE }}:${{ github.sha }}
          kubectl -n orangecrave rollout status deployment/orangecrave-admin --timeout=600s

      - name: Health check
        run: |
          sleep 60
          for i in 1 2 3; do
            curl -sf https://admin.orangecrave.example.com/admin-api/health && exit 0
            sleep 10
          done
          echo "健康检查失败，执行回滚"
          kubectl -n orangecrave rollout undo deployment/orangecrave-admin
          exit 1
```

### 4.3 发布策略

| 策略 | 说明 | 适合场景 |
|------|------|----------|
| **滚动更新** | 逐个替换 Pod，不中断服务 | 日常发布（默认） |
| **蓝绿部署** | 新旧版本同时运行，切流量 | 大版本升级 |
| **金丝雀发布** | 先放 5% 流量到新版本 | 高风险变更 |

当前 K8s Deployment 默认滚动更新。蓝绿/金丝雀需要配合 Ingress 或 Service Mesh（Istio）实现。

## 5. 变更管理

### 5.1 变更分级

| 级别 | 定义 | 审批 | 示例 |
|------|------|------|------|
| 标准变更 | 预授权的低风险变更 | 无需审批 | 依赖升级、配置调整 |
| 普通变更 | 需要评审的变更 | 技术负责人 | 新功能上线 |
| 紧急变更 | 修复生产故障 | CTO 口头批准，事后补审 | 热修复 |

### 5.2 变更检查单

每次生产发布前过一遍：

- [ ] 所有测试通过（CI 绿灯）
- [ ] 代码评审完成（至少 1 人 approve）
- [ ] staging 环境验证通过
- [ ] 数据库变更已评审（新增表/索引/字段）
- [ ] 回滚方案已准备（上一个版本镜像 tag）
- [ ] 发布窗口确认（避开高峰期）
- [ ] 相关团队已通知

## 6. 运维仪表盘

在 Grafana 中建一个「运维总览」Dashboard，一屏看到所有关键指标：

```
┌──────────────────────────────────────────────────┐
│  OrangeCrave 运维总览                              │
├──────────┬──────────┬──────────┬─────────────────┤
│  可用性   │  QPS     │  P99延迟  │  错误率          │
│  99.95%  │  1,234   │  120ms   │  0.02%          │
├──────────┴──────────┴──────────┴─────────────────┤
│  请求量趋势（1h）                                  │
├──────────┬──────────┬──────────┬─────────────────┤
│  Pod 状态 │  DB 连接  │  Redis   │  MQ 积压         │
│  3/3 ✓   │  12/20   │  正常 ✓  │  0              │
├──────────┴──────────┴──────────┴─────────────────┤
│  最近告警（24h）                                   │
│  10:23 HighLatency resolved                       │
│  08:15 ConnectionPoolHigh resolved                │
└──────────────────────────────────────────────────┘
```

## 7. 检查清单

- [ ] SLO 已定义并与团队对齐
- [ ] 告警规则已配置，覆盖 P0~P3
- [ ] 告警通知渠道已测试（手动触发一条告警）
- [ ] On-Call 手册已评审，值班表已排
- [ ] CI/CD 流水线已配置，从提交到生产全自动
- [ ] 发布策略已确定（滚动/蓝绿/金丝雀）
- [ ] 变更管理流程已建立
- [ ] 运维 Dashboard 已建好，关键指标一屏可见
