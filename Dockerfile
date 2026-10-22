# ========== 构建阶段 ==========
# 用带 Maven 的镜像：eclipse-temurin:17-jdk 里没有 mvn，直接在它上面跑 mvn 会失败
FROM maven:3.9-eclipse-temurin-17 AS builder
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
RUN mvn -B -q dependency:go-offline

# 复制源码并构建
COPY orangecrave-admin/ .
RUN mvn -B -DskipTests package

# ========== 运行阶段 ==========
FROM eclipse-temurin:17-jre

# curl 供容器健康检查使用；固定 UID 便于宿主机目录授权
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r -g 10001 orangecrave \
    && useradd -r -u 10001 -g orangecrave orangecrave \
    && mkdir -p /app/uploads \
    && chown -R 10001:10001 /app

WORKDIR /app
USER orangecrave

# 从构建阶段复制 jar
COPY --from=builder /build/orangecrave-admin-api/target/orangecrave-admin.jar app.jar

# JVM 参数：针对 2C2G 单机；日志走 stdout，由 Docker 接管
ENV JAVA_OPTS="-Xms256m -Xmx384m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Duser.timezone=Asia/Shanghai"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
