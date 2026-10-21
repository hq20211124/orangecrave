# 05 — 压测与容量验证

> 目标：知道系统能扛多少流量，瓶颈在哪，扩容到目标 QPS 需要加什么。

## 1. 压测方法论

### 1.1 原则

1. **先测单接口，再测全链路** — 先知道每个接口的极限，再组合起来测
2. **先测读，再测写** — 读接口简单，先建立基线；写接口涉及事务，复杂得多
3. **逐步加压，找拐点** — 从 10 QPS 开始，每轮翻倍，直到错误率飙升或延迟暴涨
4. **数据量要真实** — 空库和 100 万条数据的查询性能完全不同

### 1.2 工具选择

| 工具 | 适合场景 | 学习成本 |
|------|----------|----------|
| **wrk** | 简单 HTTP 压测 | 低（推荐入门） |
| **JMeter** | 复杂场景、需要 GUI | 中 |
| **k6** | 脚本化、CI 集成 | 中（推荐进阶） |
| **Gatling** | 高并发、详细报告 | 高 |

**本文用 wrk + k6 两个工具**，wrk 快速验证，k6 做正式测试。

## 2. 环境准备

### 2.1 压测环境要求

- 独立的 staging 环境，**不能在生产压测**
- 数据量与生产接近（至少 10 万订单、1 万用户）
- 压测机与被测机分开，避免压测工具自己成为瓶颈
- 网络带宽 ≥ 1Gbps（避免网络成为瓶颈）

### 2.2 准备测试数据

```sql
-- 生成 10 万测试用户
INSERT INTO ums_user (id, nickname, avatar, gender, status, create_time)
SELECT 
  100000 + n,
  CONCAT('压测用户', n),
  'https://example.com/avatar.png',
  0, 1,
  NOW() - INTERVAL FLOOR(RAND() * 365) DAY
FROM (
  SELECT @row := @row + 1 AS n
  FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
       (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t2,
       (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t3,
       (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t4,
       (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t5,
       (SELECT @row := 0) init
  LIMIT 100000
) nums;

-- 生成 10 万测试订单
INSERT INTO oms_order (id, order_sn, user_id, status, total_amount, create_time)
SELECT
  100000 + n,
  CONCAT('SO', DATE_FORMAT(NOW(), '%Y%m%d'), LPAD(n, 8, '0')),
  100000 + FLOOR(RAND() * 100000),
  FLOOR(RAND() * 8),
  ROUND(RAND() * 500 + 10, 2),
  NOW() - INTERVAL FLOOR(RAND() * 90) DAY
FROM (同上递归生成数字) nums;
```

## 3. 基准测试（wrk）

### 3.1 健康检查接口

```bash
# 最轻量的接口，测试 Tomcat 和网络的极限
wrk -t4 -c100 -d30s http://localhost:8080/admin-api/health

# 预期结果（参考）：
# 4 线程 100 连接，30 秒
# Requests/sec: ~20000+
# 这个接口没有业务逻辑，测的是 Web 容器本身的吞吐
```

### 3.2 登录接口

```bash
# 先手动登录拿到 token
TOKEN=$(curl -s -X POST http://localhost:8080/admin-api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.data.token')

# 测需要认证的接口
wrk -t4 -c100 -d30s \
  -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8080/admin-api/system/user/list?page=1&pageSize=20
```

### 3.3 订单列表（读）

```bash
wrk -t4 -c200 -d60s \
  -H "Authorization: Bearer ${TOKEN}" \
  "http://localhost:8080/admin-api/oms/order/list?page=1&pageSize=20&status=1"
```

### 3.4 下单接口（写）

```bash
# 写接口需要带 Idempotency-Key，且每次不同
# wrk 不支持动态生成，用 Lua 脚本
cat > order_submit.lua << 'EOF'
local counter = 0

request = function()
  counter = counter + 1
  local body = string.format([[{
    "items": [{"skuId": 1, "quantity": 1}],
    "addressId": 1,
    "remark": "压测订单"
  }]])
  return wrk.format("POST", nil, {
    ["Content-Type"] = "application/json",
    ["Authorization"] = "Bearer TOKEN_PLACEHOLDER",
    ["Idempotency-Key"] = "bench-" .. counter
  }, body)
end
EOF

# 替换 TOKEN_PLACEHOLDER 为实际 token 后执行
wrk -t4 -c50 -d60s -s order_submit.lua http://localhost:8080/client-api/oms/order/submit
```

### 3.5 秒杀接口（写 + 限流）

```bash
cat > seckill.lua << 'EOF'
local counter = 0

request = function()
  counter = counter + 1
  local body = string.format([[{
    "seckillId": 1,
    "skuId": 1,
    "quantity": 1
  }]])
  return wrk.format("POST", nil, {
    ["Content-Type"] = "application/json",
    ["Authorization"] = "Bearer TOKEN_PLACEHOLDER",
    ["Idempotency-Key"] = "seckill-bench-" .. counter
  }, body)
end
EOF

wrk -t8 -c500 -d60s -s seckill.lua http://localhost:8080/client-api/oms/order/seckill
```

**注意**：秒杀接口有限流（10 QPS + 20 突发），超出部分会返回 429。这正是我们要验证的——限流是否生效。

## 4. 正式压测（k6）

### 4.1 全链路场景

创建 `k6/full-flow.js`：

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// 自定义指标
const errorRate = new Rate('errors');
const orderLatency = new Trend('order_submit_latency');

// 压测配置
export const options = {
  stages: [
    { duration: '2m', target: 50 },    // 2 分钟爬到 50 并发
    { duration: '5m', target: 50 },    // 保持 5 分钟
    { duration: '2m', target: 100 },   // 爬到 100
    { duration: '5m', target: 100 },   // 保持
    { duration: '2m', target: 200 },   // 爬到 200
    { duration: '5m', target: 200 },   // 保持
    { duration: '2m', target: 0 },     // 降到 0
  ],
  thresholds: {
    http_req_duration: ['p(99)<500'],   // P99 < 500ms
    errors: ['rate<0.01'],               // 错误率 < 1%
  },
};

const BASE_URL = 'http://staging.orangecrave.example.com';
let token = '';

export function setup() {
  // 登录获取 token
  const res = http.post(`${BASE_URL}/admin-api/auth/login`, JSON.stringify({
    username: 'admin',
    password: 'admin123'
  }), { headers: { 'Content-Type': 'application/json' } });
  return { token: res.json('data.token') };
}

export default function(data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  // 场景 1：浏览商品（40% 流量）
  if (Math.random() < 0.4) {
    const res = http.get(`${BASE_URL}/client-api/pms/spu/list?page=1&pageSize=10`, { headers });
    check(res, { '商品列表 200': (r) => r.status === 200 });
  }

  // 场景 2：查看订单（30% 流量）
  else if (Math.random() < 0.7) {
    const res = http.get(`${BASE_URL}/admin-api/oms/order/list?page=1&pageSize=20`, { headers });
    check(res, { '订单列表 200': (r) => r.status === 200 });
  }

  // 场景 3：下单（20% 流量）
  else if (Math.random() < 0.9) {
    const res = http.post(`${BASE_URL}/client-api/oms/order/submit`, JSON.stringify({
      items: [{ skuId: 1, quantity: 1 }],
      addressId: 1,
      remark: 'k6 压测'
    }), {
      headers: { ...headers, 'Idempotency-Key': `k6-${__VU}-${__ITER}` }
    });
    check(res, { '下单 200': (r) => r.status === 200 });
    orderLatency.add(res.timings.duration);
  }

  // 场景 4：秒杀（10% 流量）
  else {
    const res = http.post(`${BASE_URL}/client-api/oms/order/seckill`, JSON.stringify({
      seckillId: 1,
      skuId: 1,
      quantity: 1
    }), {
      headers: { ...headers, 'Idempotency-Key': `k6-seckill-${__VU}-${__ITER}` }
    });
    check(res, {
      '秒杀响应': (r) => r.status === 200 || r.status === 429  // 429 也算正常（限流）
    });
  }

  sleep(1);  // 每个虚拟用户间隔 1 秒
}
```

### 4.2 运行

```bash
k6 run k6/full-flow.js --out json=results.json
```

### 4.3 分析结果

k6 输出示例：

```
  ✓ http_req_duration..............: avg=45ms  min=2ms  med=32ms  max=1.2s  p(90)=89ms  p(95)=120ms  p(99)=350ms
  ✓ http_req_failed................: 0.23%
  ✓ errors.........................: 0.23%
  ✓ iterations.....................: 15234
  ✓ vus............................: 200
```

**关键指标解读**：

| 指标 | 含义 | 合格标准 |
|------|------|----------|
| `p(99) < 500ms` | 99% 的请求在 500ms 内完成 | 生产要求 |
| `errors < 1%` | 错误率低于 1% | 生产要求 |
| `http_req_failed` | 连接失败/超时 | 应接近 0 |
| `iterations` | 总请求数 | 越多越好 |

## 5. 容量模型

### 5.1 单实例基线（参考值）

基于当前架构（单 Pod、4 核 8G、HikariCP 20 连接池）：

| 接口类型 | 预期 QPS | 瓶颈 |
|----------|----------|------|
| 健康检查 | 20,000+ | Tomcat 线程数 |
| 商品列表（缓存命中） | 5,000+ | 缓存读取 |
| 订单列表（走索引） | 2,000+ | MySQL 查询 |
| 下单（无秒杀） | 500+ | MySQL 写入 + 事务 |
| 秒杀 | 100+ | 限流阈值（有意限制） |

### 5.2 扩容计算

如果目标是支撑 10,000 QPS 的读 + 1,000 QPS 的写：

```
读扩容：10,000 / 2,000 = 5 个 Pod
写扩容：写无法水平扩（单库写入瓶颈）
  → 需要读写分离，读走从库
  → 或分库分表

最终方案：
- 5 个应用 Pod（读）
- 写操作仍走主库，通过消息队列削峰
- MySQL 主从，读请求路由到从库
```

## 6. 压测报告模板

每次压测后填写：

```markdown
## 压测报告

- **日期**：2026-XX-XX
- **环境**：staging（4 核 8G × 2 Pod，MySQL 4 核 16G）
- **数据量**：10 万用户、10 万订单
- **工具**：k6 v0.50.0
- **场景**：全链路混合（40% 浏览 / 30% 查订单 / 20% 下单 / 10% 秒杀）

### 结果

| 指标 | 值 | 标准 | 是否达标 |
|------|----|------|----------|
| P99 延迟 | 350ms | <500ms | ✅ |
| 错误率 | 0.23% | <1% | ✅ |
| 最大并发 | 200 | — | — |
| 总请求数 | 15,234 | — | — |
| QPS | 85 | — | — |

### 瓶颈分析

1. 数据库 CPU 在 150 并发时达到 80%，是主要瓶颈
2. JVM 堆内存稳定，无内存泄漏
3. 限流在 200 并发时触发了 1,234 次 429，符合预期

### 结论

当前配置可稳定支撑 200 并发（约 85 QPS）。
要支撑 500 并发，需要：
- 数据库升级到 8 核 32G
- 应用 Pod 扩到 4 个
- 开启 Redis 缓存（当前 LocalCache）
```

## 7. 检查清单

- [ ] staging 环境数据量与生产接近
- [ ] 健康检查接口基准 QPS 已测
- [ ] 核心读接口 P99 < 500ms
- [ ] 核心写接口错误率 < 1%
- [ ] 秒杀接口限流生效（返回 429）
- [ ] 压测期间无内存泄漏（GC 后堆内存回落）
- [ ] 压测报告已归档，瓶颈已识别
- [ ] 容量模型已建立，知道扩到什么规模需要加什么资源
