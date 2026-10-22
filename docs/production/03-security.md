# 03 — 安全加固

> 目标：敏感数据不落盘、不走 Git；API 有认证、有限流、有防护；通过安全基线检查。

## 1. 密钥管理

### 1.1 现状问题

当前配置文件中存在硬编码占位值：
- `application.yml` 的 `root/root`
- `docker-compose.yml` 的数据库密码
- 登录页预填的 `admin/admin123`

**生产环境绝对不允许这样做。**

### 1.2 方案一：K8s Secret（最小可用）

已经在上一步部署文档中用了 `kubectl create secret generic`。这是最基本的方式：
- 密钥存在 K8s etcd 中（etcd 本身需要加密，云厂商托管 K8s 默认已加密）
- 应用通过环境变量读取
- 密钥不入 Git、不落盘

**适合**：小团队、云厂商托管 K8s、快速上线。

### 1.3 方案二：External Secrets Operator + 云 KMS（推荐）

密钥存在云厂商 KMS（阿里云 KMS / AWS Secrets Manager），ESO 自动同步到 K8s Secret：

```bash
# 安装 ESO
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets -n external-secrets-system --create-namespace
```

创建 `SecretStore`（告诉 ESO 去哪取密钥）：

```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: aliyun-kms
  namespace: orangecrave
spec:
  provider:
    alibabackms:
      regionID: cn-hangzhou
      auth:
        accessKeyID:
          name: kms-credentials
          key: access-key-id
        accessKeySecret:
          name: kms-credentials
          key: access-key-secret
```

创建 `ExternalSecret`（告诉 ESO 同步哪些密钥）：

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: orangecrave-secrets
  namespace: orangecrave
spec:
  refreshInterval: 1h          # 每小时自动同步，密钥轮转后自动更新
  secretStoreRef:
    name: aliyun-kms
    kind: SecretStore
  target:
    name: orangecrave-secrets   # 同步到这个 K8s Secret
  data:
    - secretKey: DB_PASSWORD
      remoteRef:
        key: orangecrave/prod/db-password
    - secretKey: REDIS_PASSWORD
      remoteRef:
        key: orangecrave/prod/redis-password
    - secretKey: S3_SECRET_KEY
      remoteRef:
        key: orangecrave/prod/s3-secret-key
```

**好处**：
- 密钥集中管理在云 KMS，有审计日志
- 支持自动轮转（在 KMS 改密钥，1 小时内所有 Pod 自动拿到新值）
- 开发人员永远看不到明文密钥

### 1.4 密钥分级

| 级别 | 内容 | 存储 | 谁能访问 |
|------|------|------|----------|
| L1 公开 | 服务端口、超时时间 | ConfigMap | 所有人 |
| L2 内部 | 数据库连接串（不含密码） | ConfigMap | 开发+运维 |
| L3 机密 | 数据库密码、Redis 密码、API Key | K8s Secret / KMS | 仅运维 |
| L4 绝密 | 加密主密钥、证书私钥 | KMS + RBAC 严格限制 | 仅安全管理员 |

## 2. API 安全

### 2.1 认证（已有，需确认）

系统已用 Sa-Token 做认证，两套独立 loginType：
- `/admin-api/**` → `StpUtil`（管理员）
- `/client-api/**` → `StpClientUtil`（客户端用户）

**确认清单**：
- [ ] Token 有效期设置合理（管理端 2h，客户端 7d）
- [ ] Token 过期后返回 401，前端能正确跳转登录页
- [ ] 密码用 BCrypt 加密存储（已有 `BCryptPasswordEncoder`）
- [ ] 登录有失败次数限制（防暴力破解）

### 2.2 限流（已有，需确认规则）

系统已有 `RateLimitFilter`，确认生产规则：

```yaml
orangecrave:
  rate-limit:
    enabled: true
    rules:
      "/client-api/oms/order/seckill":
        permits-per-second: 10
        burst-capacity: 20
      "/client-api/auth/wx-login":
        permits-per-second: 5
        burst-capacity: 10
      "/admin-api/auth/login":
        permits-per-second: 3
        burst-capacity: 5
```

### 2.3 API 网关

在 K8s Ingress 前面加一层 API 网关（Kong / APISIX / 云厂商 API 网关），提供：

| 功能 | 说明 |
|------|------|
| TLS 终止 | HTTPS 证书在网关层处理，应用只跑 HTTP |
| 请求大小限制 | 防止大文件 DoS |
| IP 白名单/黑名单 | 管理端只允许办公网 IP |
| 请求日志 | 所有 API 调用留痕 |
| 灰度发布 | 按 Header/权重分流到不同版本 |

以 Kong 为例的 Ingress 配置：

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: orangecrave-admin
  namespace: orangecrave
  annotations:
    konghq.com/plugins: "rate-limit-admin,ip-restriction-admin"
spec:
  ingressClassName: kong
  rules:
    - host: admin.orangecrave.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: orangecrave-admin
                port:
                  number: 80
```

## 3. Web 应用防火墙（WAF）

### 3.1 云厂商 WAF（最省事）

阿里云 / AWS 都有托管 WAF，在负载均衡器前面启用即可。防护规则包括：

- SQL 注入
- XSS 跨站脚本
- 路径穿越
- 恶意扫描器
- CC 攻击（高频请求）

### 3.2 自建 ModSecurity（可选）

如果用 Nginx Ingress，可以启用 ModSecurity：

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  annotations:
    nginx.ingress.kubernetes.io/enable-modsecurity: "true"
    nginx.ingress.kubernetes.io/enable-owasp-core-rules: "true"
```

OWASP Core Rule Set 覆盖了常见的 Web 攻击模式。

## 4. 应用层安全基线

### 4.1 HTTP 安全头

在 `SaTokenConfigure` 或单独的 Filter 中添加：

```java
// 文件：common/src/main/java/com/orangecrave/admin/common/config/SecurityHeaderFilter.java
package com.orangecrave.admin.common.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头。防止点击劫持、MIME 嗅探、XSS 等常见 Web 攻击。
 */
@Component
@Order(1)
public class SecurityHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("X-Content-Type-Options", "nosniff");
        // 后台是 iframe 布局（layout.html 内嵌 pages/*），DENY 会导致同源页面也拒绝渲染
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");
        resp.setHeader("X-XSS-Protection", "1; mode=block");
        resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        resp.setHeader("Content-Security-Policy", "default-src 'self'");
        resp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        chain.doFilter(request, response);
    }
}
```

### 4.2 敏感数据加密

系统中的敏感字段需要加密存储：

| 字段 | 现状 | 需要做什么 |
|------|------|-----------|
| 用户密码 | BCrypt 哈希 | 已安全 |
| 手机号 | 明文存储 | AES-GCM 加密存储，展示时脱敏 |
| 身份证 | 明文存储 | AES-GCM 加密存储，展示时只显示后 4 位 |
| 银行卡 | 无 | 如果有，必须加密 + 脱敏 |

系统已有 `AesGcmCryptoTest`，说明 AES-GCM 加密工具已就位。需要做的是：
1. 定义哪些字段需要加密（参考 `SensitiveDataFlowTest`）
2. 在 Entity 的 getter/setter 或 MyBatis TypeHandler 中自动加密/解密
3. 展示层脱敏（`138****1234`）

### 4.3 文件上传安全

系统允许用户上传图稿，需要防止：

- **文件类型校验**：只允许 jpg/png/webp，检查 magic number 不只看扩展名
- **文件大小限制**：单文件 ≤ 10MB
- **病毒扫描**：上传后异步扫描（ClamAV），不通过则删除
- **存储隔离**：上传目录不可执行（防止上传 webshell）

### 4.4 SQL 注入

系统用 MyBatis-Plus，参数化查询天然防 SQL 注入。但需确认：
- 没有使用 `${}` 拼接 SQL（`#{}` 是安全的，`${}` 是拼接）
- 排序字段不直接拼接用户输入

```bash
# 检查是否有危险的 ${} 用法
grep -rn '\${' --include="*.java" orangecrave-admin/
```

## 5. 安全审计

### 5.1 操作日志（已有）

系统已有 `sys_oper_log` 表，记录管理端操作。确认：
- 记录操作人、操作时间、操作类型、操作内容
- 敏感操作（删除、导出、修改权限）有额外审批流
- 日志不可篡改（追加写入，不允许 UPDATE/DELETE）

### 5.2 数据访问审计

对敏感数据的访问需要记录：
- 谁查了什么用户的手机号/身份证
- 谁导出了什么数据
- 什么时候、从哪个 IP

可以在 `common/` 加一个注解 + AOP：

```java
// 标记需要审计的方法
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAudit {
    String action();          // 操作类型：查询/导出/修改
    String dataType();        // 数据类型：手机号/身份证/银行卡
}
```

## 6. 安全检查清单

上线前逐项过一遍：

- [ ] 所有密码/密钥从环境变量注入，不在配置文件中
- [ ] Git 仓库中无 `.env`、无密钥文件
- [ ] 管理端 API 有 IP 白名单（仅办公网/ VPN）
- [ ] HTTPS 全站启用，HTTP 自动跳转
- [ ] 安全响应头已配置
- [ ] 文件上传有类型+大小+病毒扫描
- [ ] 无 `${}` SQL 拼接
- [ ] 敏感字段加密存储 + 展示脱敏
- [ ] 操作日志完整
- [ ] 登录有失败锁定
- [ ] Token 过期时间合理
- [ ] 容器以非 root 用户运行
- [ ] K8s Pod 无特权模式
- [ ] 网络策略（NetworkPolicy）限制 Pod 间通信
