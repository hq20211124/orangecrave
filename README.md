# OrangeCrave 橙刻定制

一套自研的「图稿定制实体周边」业务系统：Spring Boot 3 后台 + 微信小程序演示版。业务为用户上传图稿定制小卡、亚克力、手机壳等实体周边，后台完成接单、审核、生产、发货全链路。

## 免责声明

- **品牌与主体均为虚构**：「橙刻」/ orangecrave 及「深圳市橙刻创意科技有限公司」是本项目虚构的演示品牌，与现实中任何同名或近名的商标、企业无关；本项目与其无任何关联。
- **业务数据均为演示数据**：代码与 SQL 中的手机号、姓名、证件号、地址等全部为虚构占位数据。
- **源码独立编写**：本工程源码、文档与页面文案均为独立编写，不含第三方项目的复制代码。

## 仓库组成

| 目录 | 说明 |
|------|------|
| `orangecrave-admin/` | Spring Boot 3.2.5 / Java 17 多模块 Maven 工程（13 个模块），含后台 REST API 与纯静态 MPA 管理界面 |
| `orangecrave-miniprogram/` | 微信小程序最小演示版（8 页，原生开发，无构建步骤） |
| `docs/design/` | 需求与设计文档：总览、数据库、API、订单状态机、计价营销、审核售后、前端、小程序 |
| `docs/production/` | 生产就绪指南：部署、可观测、安全、数据库、压测、容灾、外部集成、运维 |
| `deploy/` | 部署脚本与 Kubernetes 清单 |
| `k6/` | 压测脚本（k6 + wrk） |
| `sql/` | 建库脚本（见 `orangecrave-admin/sql/`） |

## 快速开始

依赖：JDK 17、Maven 3.9、MySQL 8（或 Docker）。

```bash
# 1. 起数据库（MySQL 8 + Redis + MinIO）
cd orangecrave-admin
docker compose up -d mysql

# 2. 构建并运行（端口 8080）
mvn -DskipTests package
java -jar orangecrave-admin-api/target/orangecrave-admin.jar

# 3. 跑全量测试（H2 内存库，无外部依赖）
mvn -pl orangecrave-admin-api -am test
```

- 后台管理：`http://localhost:8080/admin/`（登录页预填 `admin`/`admin123`，真实管理员密码由 `DataInitializer` 启动时生成）
- 健康检查：`GET /admin-api/health`

## 架构要点

- **认证**：Sa-Token 双体系（管理端 `StpUtil` / 客户端 `StpClientUtil`）
- **持久层**：MyBatis-Plus，逻辑删除，实体继承统一 `BaseEntity`
- **可插拔支撑层**：缓存 / 分布式锁 / 限流 / 消息队列 / 对象存储均为接口抽象，本地实现默认可用，Redis / S3 实现通过开关切换（`orangecrave.redis.enabled`、`orangecrave.storage.type`）
- **秒杀削峰**：`StockGate` 库存预扣闸门 + 异步下单受理（提交即返回 requestId，消费者落地订单）
- **测试**：154 个测试用例，H2 内存库运行，不依赖外部中间件

## 文档

- 设计文档入口：`docs/design/README.md`
- 生产就绪：`docs/production/00-overview.md`
- 第三方依赖许可：`THIRD-PARTY-NOTICES.md`

## License

[Apache License 2.0](LICENSE)
