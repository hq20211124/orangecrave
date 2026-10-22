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

# 创建非 root 用户
RUN groupadd -r orangecrave && useradd -r -g orangecrave orangecrave
USER orangecrave

# 从构建阶段复制 jar
COPY --from=builder /build/orangecrave-admin-api/target/orangecrave-admin.jar app.jar

# JVM 参数：堆内存、GC、时区
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Duser.timezone=Asia/Shanghai"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
