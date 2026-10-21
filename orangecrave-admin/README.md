# orangecrave-admin

橙刻定制后台管理系统 —— 实现工程（设计见 [`../docs/design`](../docs/design)）。
- 技术栈：Spring Boot 3.2.5 · Java 17 · MyBatis-Plus 3.5.5 · Sa-Token 1.37.0 · MySQL 8 · 原生 HTML/JS
- groupId `com.orangecrave`，基础化 `com.orangecrave.admin`
- 模块结构见设计 `00-overview.md` §6

## 快速开始
### 1. 起数据库（MySQL 8）
```bash
docker compose up -d mysql
```

首次启动会按顺序执行 `sql/*_schema.sql` 且 `sql/*_seed.sql`。连接串见 `orangecrave-admin-api/src/main/resources/application.yml`（默认 `localhost:3306/orangecrave`，root/root）。
> 本机无 Docker 时，自行准备 MySQL 8 后手动执行 `sql/` 下脚本即可。
### 2. 编译与运行
```bash
mvn -DskipTests package
java -jar orangecrave-admin-api/target/orangecrave-admin.jar
```

启动后可访问 `GET /admin-api/health`。
### 3. 默认账号

首次启动用 `DataInitializer` 创建，
- 用户名 `admin`
- 密码 `admin123`

**请登录后立即修改。**

## 测试

```bash
mvn -pl orangecrave-admin-api -am test
```

`AdminApplicationTest` 用 H2（MODE=MySQL）起完整上下文，覆盖：健康检查 → 登录 → 带 token 取用户信息 → 菜单 → 未授权 401。无需外部数据库。
## 进度

| 模块 | 状态 |
|---|---|
| common 支撑层 | ✅ |
| system（认证/权限/日志/配置） | ✅ |
| goods（商品/计价/运费） | ✅ |
| task（延迟任务引擎） | ✅ |
| order（状态机/桩 发货/售后/发票） | ✅ |
| work（作品/合集/分享码） | ✅ |
| audit（pHash/敏感词/版权承诺） | ✅ |
| marketing（活动/拼团/优惠券） | ✅ |
| service（工单四态） | ✅ |
| finance（日结/看板/对账） | ✅ |
| user（小程序用户/认证/渠道） | ✅ |
| 顾客端 client 接口（02 §2.13） | ✅ |
| admin-web 前端（登录/布局/待办/订单/商品/审核/参数/日志） | ✅ |

> 后端 13 模块 + 原生前端完成，**端到端主链路已由测试验证**（`ClientFlowTest`）。
> 小程序演示版见 [`../orangecrave-miniprogram`](../orangecrave-miniprogram)。
## 前端

`orangecrave-admin-web` 打包为静态资源，随应用托管在 `/admin/`。
- 登录页 `/admin/index.html`（默认 admin / admin123）
- 主框架 `/admin/layout.html`（左侧菜单由 `/admin-api/auth/menus` 动态渲染 + 内容 iframe）
- 页面：运营待办、订单列表/详情、商品列表、审核队列、系统参数、操作日志