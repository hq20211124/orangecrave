# 02 — 可观测性：Metrics、日志、链路追踪

> 目标：出了问题 30 秒内能在 Dashboard 上看到异常，5 分钟内定位到具体接口和实例。

## 1. 整体架构

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Spring Boot  │────→│  Prometheus  │────→│   Grafana    │
│  (metrics)   │     │  (采集存储)   │     │  (Dashboard) │
└──────────────┘     └──────────────┘     └──────────────┘
       │
       │              ┌──────────────┐     ┌──────────────┐
       ├─────────────→│    Loki      │────→│   Grafana    │
       │  (日志)      │  (日志聚合)   │     │  (日志查询)   │
       │              └──────────────┘     └──────────────┘
       │
       │              ┌──────────────┐
       └─────────────→│    Tempo     │     ┌──────────────┐
          (Trace)     │  (链路追踪)   │────→│   Grafana    │
                      └──────────────┘     │  (Trace 查询)│
                                            └──────────────┘
```

**三个信号，一个 Dashboard**：Grafana 统一展示 Metrics + 日志 + Trace，按 traceId 互相关联。

## 2. 接入 Prometheus Metrics

### 2.1 添加依赖

在 `orangecrave-common/pom.xml` 中：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2.2 配置

`application.yml` 追加：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized
    prometheus:
      enabled: true
  metrics:
    tags:
      application: orangecrave-admin    # 所有指标打应用标签
```

### 2.3 K8s 侧：Prometheus 抓取配置

在 Deployment 的 Pod template 上加 annotation：

```yaml
template:
  metadata:
    annotations:
      prometheus.io/scrape: "true"
      prometheus.io/port: "8080"
      prometheus.io/path: "/actuator/prometheus"
```

如果用 Helm 安装 Prometheus（kube-prometheus-stack），ServiceMonitor 更规范：

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: orangecrave-admin
  namespace: orangecrave
spec:
  selector:
    matchLabels:
      app: orangecrave-admin
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 15s
```

### 2.4 验证

```bash
# 本地验证
curl http://localhost:8080/actuator/prometheus | head -50

# 应该能看到类似：
# jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 1.23E8
# http_server_requests_seconds_count{method="GET",status="200",uri="/admin-api/health",} 42
```

## 3. 关键指标

### 3.1 系统自带（零代码）

Spring Boot Actuator + Micrometer 自动采集：

| 指标 | 含义 | 告警阈值 |
|------|------|----------|
| `http_server_requests_seconds` | HTTP 请求延迟分布 | P99 > 500ms |
| `http_server_requests_seconds_count` | 请求 QPS | 错误率 > 1% |
| `jvm_memory_used_bytes` | JVM 内存使用 | 堆使用 > 80% |
| `jvm_gc_pause_seconds` | GC 暂停时间 | 单次 > 200ms |
| `hikaricp_connections_active` | 数据库连接池活跃数 | 接近 maxPoolSize |
| `hikaricp_connections_pending` | 连接池等待队列 | > 0（说明池不够） |
| `process_cpu_usage` | 进程 CPU 使用率 | > 80% |

### 3.2 业务自定义指标（需要加少量代码）

在 `common/` 模块新增一个简单的指标收集器：

```java
// 文件：common/src/main/java/com/orangecrave/admin/common/metrics/BusinessMetrics.java
package com.orangecrave.admin.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * 业务指标统一入口。所有自定义指标从这里注册，命名前缀 orangecrave_。
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** 订单提交计数：tag=status(success/fail) */
    public Counter orderSubmit(String status) {
        return Counter.builder("orangecrave_order_submit_total")
                .tag("status", status)
                .register(registry);
    }

    /** 秒杀预扣计数：tag=result(pass/reject) */
    public Counter seckillReserve(String result) {
        return Counter.builder("orangecrave_seckill_reserve_total")
                .tag("result", result)
                .register(registry);
    }

    /** 限流触发计数：tag=path */
    public Counter rateLimitHit(String path) {
        return Counter.builder("orangecrave_rate_limit_hit_total")
                .tag("path", path)
                .register(registry);
    }

    /** MQ 消息投递延迟 */
    public Timer mqPublishLatency() {
        return Timer.builder("orangecrave_mq_publish_seconds")
                .register(registry);
    }
}
```

**验证**：启动后访问 `http://localhost:8080/actuator/prometheus`，搜索 `orangecrave_` 前缀确认指标已注册。

## 4. Grafana Dashboard

### 4.1 安装

用 Helm 安装完整可观测栈：

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Prometheus + Grafana + Alertmanager 一体安装
helm install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace \
  --set grafana.adminPassword='初始密码'
```

### 4.2 核心面板（导入 JSON 或手动建）

**面板 1：系统总览**

| 图表 | PromQL |
|------|--------|
| QPS | `rate(http_server_requests_seconds_count{application="orangecrave-admin"}[1m])` |
| P99 延迟 | `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{application="orangecrave-admin"}[5m]))` |
| 错误率 | `rate(http_server_requests_seconds_count{application="orangecrave-admin",status=~"5.."}[1m]) / rate(http_server_requests_seconds_count{application="orangecrave-admin"}[1m])` |
| 活跃实例数 | `count(up{application="orangecrave-admin"} == 1)` |

**面板 2：JVM**

| 图表 | PromQL |
|------|--------|
| 堆内存 | `jvm_memory_used_bytes{application="orangecrave-admin",area="heap"}` |
| GC 暂停 | `rate(jvm_gc_pause_seconds_sum{application="orangecrave-admin"}[1m])` |
| 线程数 | `jvm_threads_live_threads{application="orangecrave-admin"}` |

**面板 3：数据库**

| 图表 | PromQL |
|------|--------|
| 连接池活跃 | `hikaricp_connections_active{application="orangecrave-admin"}` |
| 连接池等待 | `hikaricp_connections_pending{application="orangecrave-admin"}` |
| 慢查询数 | `rate(mysql_global_status_slow_queries[5m])`（需要 MySQL exporter） |

**面板 4：业务**

| 图表 | PromQL |
|------|--------|
| 订单提交 QPS | `rate(orangecrave_order_submit_total[1m])` |
| 秒杀预扣通过率 | `rate(orangecrave_seckill_reserve_total{result="pass"}[1m]) / rate(orangecrave_seckill_reserve_total[1m])` |
| 限流触发次数 | `rate(orangecrave_rate_limit_hit_total[5m])` |

## 5. 日志聚合（Loki）

### 5.1 为什么不用 ELK

ELK（Elasticsearch + Logstash + Kibana）资源开销大（ES 至少 2GB 堆），运维复杂。Loki 是 Grafana Labs 的日志系统，只索引标签不索引全文，资源开销小一个数量级，和 Grafana 天然集成。

### 5.2 安装

```bash
helm install loki grafana/loki-stack \
  --namespace monitoring \
  --set promtail.enabled=true
```

Promtail 会自动以 DaemonSet 部署到每个 K8s 节点，采集所有容器日志。

### 5.3 应用日志格式

在 `application.yml` 中配置结构化日志（JSON 格式，方便 Loki 解析）：

```yaml
logging:
  pattern:
    console: '{"time":"%d{yyyy-MM-dd HH:mm:ss.SSS}","level":"%level","thread":"%thread","logger":"%logger{36}","msg":"%msg","traceId":"%X{traceId}","userId":"%X{userId}"}%n'
```

**注意**：`%X{traceId}` 来自 MDC，需要链路追踪组件（见下文）自动写入。如果没接入 Trace，这个字段为空，不影响日志输出。

### 5.4 在 Grafana 中查询日志

Grafana → Explore → 数据源选 Loki：

```logql
# 查最近 5 分钟的 ERROR 日志
{namespace="orangecrave"} |= "ERROR"

# 按 traceId 串联一次请求的所有日志
{namespace="orangecrave"} |= "traceId=abc123"

# 按标签过滤
{namespace="orangecrave", app="orangecrave-admin", level="ERROR"}
```

## 6. 链路追踪（Tempo + OpenTelemetry）

### 6.1 添加依赖

在 `orangecrave-common/pom.xml`：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

### 6.2 配置

```yaml
management:
  tracing:
    sampling:
      probability: 0.1    # 生产采样 10%，全量会爆存储
  otlp:
    tracing:
      endpoint: http://tempo.monitoring:4317
```

### 6.3 安装 Tempo

```bash
helm install tempo grafana/tempo --namespace monitoring
```

### 6.4 效果

一次 HTTP 请求 → Spring MVC → Service → MyBatis → MySQL，全链路耗时在 Grafana 的 Trace 面板中可视化。每个 Span 显示：
- 哪个接口
- 哪个 Service 方法
- 执行了什么 SQL
- 每段耗时多少

**Metrics 告诉你「慢了」，Trace 告诉你「慢在哪」。**

## 7. 告警规则（Alertmanager）

创建 `deploy/alerts/orangecrave-alerts.yaml`：

```yaml
groups:
  - name: orangecrave
    rules:
      # 错误率告警
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{application="orangecrave-admin",status=~"5.."}[5m]) / rate(http_server_requests_seconds_count{application="orangecrave-admin"}[5m]) > 0.01
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "错误率超过 1%"
          description: "最近 5 分钟错误率 {{ $value | humanizePercentage }}"

      # P99 延迟告警
      - alert: HighLatency
        expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{application="orangecrave-admin"}[5m])) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "P99 延迟超过 500ms"
          description: "P99 延迟 {{ $value }}s"

      # 实例下线
      - alert: InstanceDown
        expr: up{application="orangecrave-admin"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "实例 {{ $labels.instance }} 已下线"

      # 数据库连接池耗尽
      - alert: ConnectionPoolExhausted
        expr: hikaricp_connections_pending{application="orangecrave-admin"} > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "数据库连接池有等待，可能耗尽"
```

## 验证清单

- [ ] `curl http://localhost:8080/actuator/prometheus` 返回指标数据
- [ ] Grafana 能看到 QPS / 延迟 / 错误率 曲线
- [ ] Loki 能查到应用日志，且含 traceId
- [ ] Tempo 能看到完整调用链
- [ ] 手动触发一次错误，确认告警能发出
- [ ] 从 Grafana 的 Metrics 面板点击一个 traceId，能跳到 Trace 详情
