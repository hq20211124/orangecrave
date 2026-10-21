# 01 — 部署容器化与多环境管理

> 目标：`docker build` 一条命令出镜像，`kubectl apply` 一条命令拉起完整环境。

## 1. Dockerfile

在项目根目录（`orangecrave-admin/` 的上一级，即仓库根）创建 `Dockerfile`：

```dockerfile
# ========== 构建阶段 ==========
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build

# 先复制 pom 文件，利用 Docker 缓存层
COPY orangecrave-admin/pom.xml .
COPY orangecrave-admin/orangecrave-common/pom.xml orangecrave-common/
COPY orangecrave-admin/orangecrave-system/pom.xml orangecrave-system/
COPY orangecrave-admin/orangecrave-user/pom.xml orangecrave-user/
COPY orangecrave-admin/orangecrave-goods/pom.xml orangecrave-goods/
COPY orangecrave-admin/orangecrave-work/pom.xml orangecrave-work/
COPY orangecrave-admin/orangecrave-order/pom.xml orangecrave-order/
COPY orangecrave-admin/orangecrave-audit/pom.xml orangecrave-audit/
COPY orangecrave-admin/orangecrave-marketing/pom.xml orangecrave-marketing/
COPY orangecrave-admin/orangecrave-service/pom.xml orangecrave-service/
COPY orangecrave-admin/orangecrave-finance/pom.xml orangecrave-finance/
COPY orangecrave-admin/orangecrave-task/pom.xml orangecrave-task/
COPY orangecrave-admin/orangecrave-admin-api/pom.xml orangecrave-admin-api/
COPY orangecrave-admin/orangecrave-admin-web/pom.xml orangecrave-admin-web/

# 下载依赖（这一步会被缓存，除非 pom 变了）
RUN mvn dependency:go-offline -B

# 复制源码并构建
COPY orangecrave-admin/ .
RUN mvn package -DskipTests -B

# ========== 运行阶段 ==========
FROM eclipse-temurin:17-jre
WORKDIR /app

# 创建非 root 用户（安全基线，见 03-security）
RUN groupadd -r orangecrave && useradd -r -g orangecrave orangecrave
USER orangecrave

# 从构建阶段复制 jar
COPY --from=builder /build/orangecrave-admin-api/target/orangecrave-admin.jar app.jar

# JVM 参数：堆内存、GC、时区
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Duser.timezone=Asia/Shanghai"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**验证**：
```bash
docker build -t orangecrave-admin:latest .
docker run -p 8080:8080 orangecrave-admin:latest
# 浏览器访问 http://localhost:8080/admin-api/health 应该返回 {"code":0}
```

## 2. .dockerignore

仓库根目录创建 `.dockerignore`：

```
.git
.tmpwx
**/target
**/.idea
**/node_modules
*.md
docs
.scratch
.workbuddy
```

## 3. 多环境配置

现在只有一个 `application.yml`。生产需要按环境拆分：

```
orangecrave-admin-api/src/main/resources/
├── application.yml              # 公共配置（不含任何环境敏感值）
├── application-dev.yml          # 开发环境
├── application-staging.yml      # 预发环境（镜像生产配置，规模缩小）
└── application-prod.yml         # 生产环境
```

### application.yml（公共，不含敏感值）

保留现有的非敏感配置。把所有环境差异项移到各环境文件。

### application-prod.yml（生产模板）

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=true&requireSSL=true&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}    # 从环境变量注入，不写死
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:20}
      minimum-idle: ${DB_POOL_MIN:5}
      connection-timeout: 3000
      max-lifetime: 1800000     # 30 分钟，避免 MySQL wait_timeout 断开

orangecrave:
  redis:
    enabled: true
  storage:
    type: s3
  cache:
    ttl-seconds: 300            # 生产缓存 5 分钟
    max-size: 10240
  stock:
    ttl-hours: 24
  rate-limit:
    enabled: true
  mq:
    stream-prefix: "orangecrave:stream:"

server:
  port: 8080
  shutdown: graceful            # 优雅停机，见下文

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

**关键原则：所有环境差异值一律从环境变量注入（`${VAR:default}`），不在配置文件里写死任何 IP、密码、密钥。**

## 4. Kubernetes 部署

### 4.1 目录结构

```
deploy/
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml             # 手动创建，不入 Git
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   └── hpa.yaml                # 自动扩缩容
└── helm/                       # 可选：Helm Chart 模板
```

### 4.2 namespace.yaml

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: orangecrave
```

### 4.3 configmap.yaml

非敏感配置放这里：

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: orangecrave-config
  namespace: orangecrave
data:
  SPRING_PROFILES_ACTIVE: "prod"
  DB_HOST: "mysql-primary.orangecrave.svc.cluster.local"
  DB_PORT: "3306"
  DB_NAME: "orangecrave"
  DB_POOL_SIZE: "20"
  REDIS_HOST: "redis-master.orangecrave.svc.cluster.local"
  REDIS_PORT: "6379"
  S3_ENDPOINT: "https://oss-cn-hangzhou.aliyuncs.com"
  S3_BUCKET: "orangecrave-prod"
```

### 4.4 secret.yaml

**敏感配置不入 Git。** 用 `kubectl` 手动创建：

```bash
kubectl create namespace orangecrave

kubectl create secret generic orangecrave-secrets \
  --namespace=orangecrave \
  --from-literal=DB_USERNAME=orangecrave_prod \
  --from-literal=DB_PASSWORD='你的数据库密码' \
  --from-literal=REDIS_PASSWORD='你的Redis密码' \
  --from-literal=S3_ACCESS_KEY='你的AK' \
  --from-literal=S3_SECRET_KEY='你的SK'
```

如果团队用 Sealed Secrets 或 External Secrets Operator，把加密后的值入 Git。见 [03-security](03-security.md)。

### 4.5 deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: orangecrave-admin
  namespace: orangecrave
  labels:
    app: orangecrave-admin
spec:
  replicas: 2                    # 至少 2 副本，滚动更新时不中断服务
  revisionHistoryLimit: 3        # 保留 3 个历史版本，方便回滚
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1                # 滚动时最多多 1 个 Pod
      maxUnavailable: 0          # 滚动时不允许有不可用 Pod
  selector:
    matchLabels:
      app: orangecrave-admin
  template:
    metadata:
      labels:
        app: orangecrave-admin
    spec:
      containers:
        - name: app
          image: registry.example.com/orangecrave/admin:v1.0.0
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: orangecrave-config
            - secretRef:
                name: orangecrave-secrets
          resources:
            requests:
              cpu: 500m
              memory: 1Gi
            limits:
              cpu: 1000m
              memory: 2Gi
          readinessProbe:
            httpGet:
              path: /admin-api/health
              port: 8080
            initialDelaySeconds: 30    # Spring Boot 启动慢，给够时间
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          livenessProbe:
            httpGet:
              path: /admin-api/health
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
            timeoutSeconds: 5
            failureThreshold: 3
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 15"]  # 给负载均衡器时间摘除节点
```

**关键点解释**：

- `maxUnavailable: 0` — 滚动更新期间不允许有 Pod 不可用，保证服务不中断
- `readinessProbe` — Pod 没就绪时不接流量。Spring Boot 启动慢，`initialDelaySeconds` 要给够
- `preStop sleep 15` — K8s 先执行 preStop 再发 SIGTERM，给 Ingress/Service 时间摘除这个 Pod
- `resources` — 不设 requests/limits 会导致 Pod 被调度到资源不足的节点

### 4.6 service.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: orangecrave-admin
  namespace: orangecrave
spec:
  selector:
    app: orangecrave-admin
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
```

### 4.7 ingress.yaml

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: orangecrave-admin
  namespace: orangecrave
  annotations:
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"     # 上传文件大小限制
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
spec:
  ingressClassName: nginx
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
  tls:
    - hosts:
        - admin.orangecrave.example.com
      secretName: orangecrave-tls    # 提前用 cert-manager 或手动创建
```

### 4.8 hpa.yaml（自动扩缩容）

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: orangecrave-admin
  namespace: orangecrave
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: orangecrave-admin
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70      # CPU 超 70% 就扩容
```

## 5. 一键部署脚本

创建 `deploy/deploy.sh`：

```bash
#!/bin/bash
set -euo pipefail

IMAGE_TAG=${1:?用法: ./deploy.sh <镜像tag>，例如 ./deploy.sh v1.0.0}

echo ">>> 构建镜像"
docker build -t registry.example.com/orangecrave/admin:${IMAGE_TAG} .
docker push registry.example.com/orangecrave/admin:${IMAGE_TAG}

echo ">>> 更新 K8s Deployment"
kubectl -n orangecrave set image deployment/orangecrave-admin \
  app=registry.example.com/orangecrave/admin:${IMAGE_TAG}

echo ">>> 等待滚动更新完成"
kubectl -n orangecrave rollout status deployment/orangecrave-admin --timeout=300s

echo ">>> 验证"
kubectl -n orangecrave get pods -l app=orangecrave-admin
curl -s https://admin.orangecrave.example.com/admin-api/health | grep '"code":0' \
  && echo ">>> 部署成功" \
  || (echo ">>> 健康检查失败，执行回滚"; kubectl -n orangecrave rollout undo deployment/orangecrave-admin; exit 1)
```

## 6. 环境清单

| 环境 | 用途 | K8s Namespace | 数据库 | Redis |
|------|------|---------------|--------|-------|
| dev | 开发自测 | orangecrave-dev | dev MySQL 实例 | dev Redis |
| staging | 预发验证 | orangecrave-staging | staging MySQL（与生产同规格） | staging Redis |
| prod | 生产 | orangecrave | 生产 MySQL 主从 | 生产 Redis 集群 |

**每个环境的 ConfigMap/Secret 独立管理，不共用。**

## 验证清单

部署完成后，逐项检查：

- [ ] `kubectl get pods -n orangecrave` 所有 Pod 状态为 Running
- [ ] `curl https://admin.orangecrave.example.com/admin-api/health` 返回 `{"code":0}`
- [ ] 管理后台能正常打开并登录
- [ ] 滚动更新一次，确认无请求丢失
- [ ] `kubectl describe pod <pod>` 确认资源限制已生效
- [ ] HPA 生效：`kubectl get hpa -n orangecrave` 能看到当前副本数和目标利用率
