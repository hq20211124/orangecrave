# 02 · 接口设计

## 1. 通用规范

### 1.1 前缀与版本

```
/admin-api/{module}/{action}
```

v1 不引入 URL 版本号；破坏性变更通过新增 action 名而非改旧接口。

另有一个不带权限的探活端点 `GET /admin-api/health`（返回 `{"code":0,"data":"UP"}`），供部署健康检查与本地启动验证用，不属于业务接口。

### 1.2 统一响应体

与顾客端保持一致：

```json
{
  "code": 0,
  "msg": "success",
  "data": {}
}
```

| code | 含义 | 前端行为 |
|---|---|---|
| 0 | 成功 | 正常处理 `data` |
| 401 | 未登录 / token 失效 | **跳转登录页** |
| 403 | 无权限 | 提示无权限 |
| 404 | 资源不存在 | 提示 |
| 429 | 限流拒绝（入口令牌桶） | 提示「稍后再试」，可自动重试 |
| 500 | 服务端异常 | 提示 + 记录 |
| 1001–1099 | 业务校验失败 | 弹出 `msg` |
| 2001–2099 | 订单相关错误 | 按业务处理 |
| 3001–3099 | 审核相关错误 | 按业务处理 |

> **保留 `code = 1000` 语义**：顾客端用 1000 表示刷新 token。后台 v1 不需要，但**不占用该值**，避免将来两端合并时冲突。

### 1.3 分页

**请求**：

```
GET /admin-api/order/page?pageNum=1&pageSize=20&keyword=xxx&status=1
```

**响应**：

```json
{
  "code": 0,
  "msg": "success",
  "data": {
    "total": 128,
    "pageNum": 1,
    "pageSize": 20,
    "list": []
  }
}
```

- `pageNum` 从 1 开始
- `pageSize` 默认 20，**上限 200**
- 排序统一 `orderBy` + `orderDirection`（`asc` / `desc`），**字段名走白名单校验**，防 SQL 注入

### 1.4 认证

```
Authorization: Bearer <token>
```

Sa-Token 自定义 token 读取名以适配【见 00 篇 4.1】。

**权限注解**：

```java
@SaCheckPermission("order:refund:force")     // 按钮级
@SaCheckPermission("order:list")             // 菜单级
```

**权限标识命名**：`{模块}:{资源}:{动作}`，如 `order:refund:force`、`goods:price:update`、`audit:image:approve`。

### 1.5 其他约定

- 时间统一 `yyyy-MM-dd HH:mm:ss` 字符串
- 金额统一字符串传输，避免 JS 浮点精度问题（`"12.50"`）
- 所有写操作**必须携带幂等键**（`Idempotency-Key` 头），服务端用它 + 业务唯一键双重去重
- 敏感操作（强制退款、改价、审核）**服务端强制记录操作日志快照**，不依赖前端传参

---

## 2. 模块与接口清单

> 下列为 v1 接口设计。标记 ✱ 的是**敏感操作**，需按钮级权限 + 操作日志快照。

### 2.1 认证 `auth`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin-api/auth/login` | 账号密码登录 |
| POST | `/admin-api/auth/logout` | 登出 |
| GET | `/admin-api/auth/info` | 当前登录者信息 + 权限列表 |
| GET | `/admin-api/auth/menus` | 当前用户可见菜单树 |

### 2.2 系统管理 `system`

**管理员**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/system/user/page` | 分页 |
| GET | `/admin-api/system/user/{id}` | 详情 |
| POST | `/admin-api/system/user` | 新增 |
| PUT | `/admin-api/system/user` | 修改 |
| DELETE | `/admin-api/system/user/{id}` | 删除 |
| PUT | `/admin-api/system/user/{id}/status` | 启停 |
| ✱ PUT | `/admin-api/system/user/{id}/password/reset` | 重置密码 |
| PUT | `/admin-api/system/user/{id}/roles` | 分配角色 |

**角色**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/system/role/page` | 分页 |
| GET | `/admin-api/system/role/list` | 全量列表（分配角色下拉框数据源） |
| POST/PUT/DELETE | `/admin-api/system/role` | 增改删 |
| GET | `/admin-api/system/role/{id}/menus` | 角色已有权限 |
| ✱ PUT | `/admin-api/system/role/{id}/menus` | 分配权限 |

**菜单**

| 方法 | 路径 |
|---|---|
| GET | `/admin-api/system/menu/tree` |
| POST/PUT/DELETE | `/admin-api/system/menu` |

**系统参数配置**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/system/config/page` | 分页 |
| GET | `/admin-api/system/config/{key}` | 按 key 取值 |
| ✱ PUT | `/admin-api/system/config` | 修改（记快照） |
| ✱ PUT | `/admin-api/system/config/batch` | 批量修改 |

> 顾客端已有 `sys/selectParamsConfigByKey` 机制，后台此项与之对齐。

**日志**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/system/oper-log/page` | 操作日志（含改前后快照） |
| GET | `/admin-api/system/oper-log/{id}` | 日志详情（含 JSON 快照对比） |
| GET | `/admin-api/system/login-log/page` | 登录日志 |
| GET | `/admin-api/system/oper-log/export` | 导出 |

### 2.3 用户 `ums`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/ums/user/page` | 小程序用户分页（支持按 openid / 手机 / 身份类型筛） |
| GET | `/admin-api/ums/user/{id}` | 详情（含订单数、消费额） |
| GET | `/admin-api/ums/user/{id}/orders` | 其订单 |
| ✱ PUT | `/admin-api/ums/user/{id}/status` | 封禁 / 解封 |

**身份认证（团长 / 摊主）**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/ums/cert/page` | 认证申请分页 |
| GET | `/admin-api/ums/cert/{id}` | 详情（含材料，身份证号脱敏） |
| ✱ POST | `/admin-api/ums/cert/{id}/approve` | 通过 → **自动发打样券** |
| ✱ POST | `/admin-api/ums/cert/{id}/reject` | 驳回（必填理由） |

**收货地址**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/ums/address/page` | 查询（只读，用于客服核对） |

> 后台**不修改**用户地址——订单已存快照，改地址无意义且会污染数据。

**渠道**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/ums/channel/list` | 渠道列表 |
| POST/PUT | `/admin-api/ums/channel` | 增改 |

### 2.4 商品 `pms`

**系列 / 品类**

| 方法 | 路径 |
|---|---|
| GET | `/admin-api/pms/series/page` · `GET /{id}` · `GET /list`（下拉数据源） |
| POST/PUT/DELETE | `/admin-api/pms/series` |
| GET | `/admin-api/pms/category/tree` · `GET /list` · `GET /{id}` |
| POST/PUT/DELETE | `/admin-api/pms/category` |

**SPU**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/pms/spu/page` | 分页（按系列/品类/状态/名称筛） |
| GET | `/admin-api/pms/spu/{id}` | 详情（含 SKU、工艺） |
| GET | `/admin-api/pms/spu/{id}/skus` | 该 SPU 的 SKU 列表（编辑页规格表格数据源） |
| POST/PUT | `/admin-api/pms/spu` | 增改 |
| DELETE | `/admin-api/pms/spu/{id}` | 删除 |
| PUT | `/admin-api/pms/spu/{id}/status` | 上下架 |
| ✱ PUT | `/admin-api/pms/spu/{id}/price` | **改价（记快照）** |

**SKU**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/pms/sku/list?spuId=` | 列表 |
| POST/PUT/DELETE | `/admin-api/pms/sku` | 增改删 |
| ✱ PUT | `/admin-api/pms/sku/{id}/price` | 单 SKU 改价（记快照） |
| ✱ PUT | `/admin-api/pms/sku/{id}/stock` | 改库存（记快照） |
| ✱ POST | `/admin-api/pms/sku/batch-price` | 批量改价（记快照） |

**规格 / 工艺 / 模板**

| 方法 | 路径 |
|---|---|
| GET/POST/PUT/DELETE | `/admin-api/pms/spec-group` |
| GET/POST/PUT/DELETE | `/admin-api/pms/spec-value` |
| GET/POST/PUT/DELETE | `/admin-api/pms/craft` |
| GET | `/admin-api/pms/craft/list` · `GET /page` | 列表 / 分页（下拉与分页两个数据源） |
| GET/POST/PUT/DELETE | `/admin-api/pms/template` |

**运费模板**

| 方法 | 路径 |
|---|---|
| GET | `/admin-api/pms/freight/page` |
| POST/PUT/DELETE | `/admin-api/pms/freight` |

**画师**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/pms/designer/page` | 含**销量统计**（只统计不分成） |
| GET | `/admin-api/pms/designer/list` | 全量列表（商品编辑页画师下拉） |
| POST/PUT/DELETE | `/admin-api/pms/designer` |
| GET | `/admin-api/pms/designer/{id}/sales` | 其作品销售统计 |

### 2.5 作品与分享 `wms`

**作品**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/wms/work/page` | 分页（按状态/作者/关键词） |
| GET | `/admin-api/wms/work/{id}` | 详情（含图槽位） |
| GET | `/admin-api/wms/work/{id}/images` | 作品的图槽位图片列表 |
| ✱ PUT | `/admin-api/wms/work/{id}/status` | 强制改状态（记快照） |
| DELETE | `/admin-api/wms/work/{id}` | 删除 |

**合集**

| 方法 | 路径 |
|---|---|
| GET | `/admin-api/wms/collection/page` |
| GET | `/admin-api/wms/collection/{id}` |
| GET | `/admin-api/wms/collection/{id}/items` | 合集内的作品条目 |
| DELETE | `/admin-api/wms/collection/{id}` |

**分享码**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/wms/share-code/page` | 分页（按状态/类型/分享者） |
| GET | `/admin-api/wms/share-code/{id}` | 详情含**预付核销情况** |
| ✱ PUT | `/admin-api/wms/share-code/{id}/invalidate` | 强制失效（记快照） |
| ✱ POST | `/admin-api/wms/share-code/{id}/refund` | **退回未核销预付款** |

**打样**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/wms/proof/page` | 打样单分页（即 `order_type=2` 的订单） |

### 2.6 订单 `oms`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/oms/order/page` | 分页（多条件） |
| GET | `/admin-api/oms/order/{id}` | 详情（含明细、图槽位、状态流转） |
| GET | `/admin-api/oms/order/{id}/timeline` | **状态流转时间线**（客服答疑用） |
| GET | `/admin-api/oms/order/{id}/items` | 订单明细行（按行售后/退款的定位依据） |
| ✱ PUT | `/admin-api/oms/order/{id}/status` | **手工改状态（记快照）** |
| ✱ PUT | `/admin-api/oms/order/{id}/price` | 改价（记快照） |
| PUT | `/admin-api/oms/order/{id}/remark` | 备注 |
| ✱ PUT | `/admin-api/oms/order/{id}/pay` | **代客支付**（客服场景，支付桩） |
| POST | `/admin-api/oms/order/batch-export` | 批量导出（发货用） |

**生产**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/oms/production/page` | 生产工单分页 |
| PUT | `/admin-api/oms/production/{id}/status` | **推进生产状态**（仓管） |
| PUT | `/admin-api/oms/production/{id}/audit-pass` | 产前样审核通过 |
| POST | `/admin-api/oms/production/{id}/work-order-no` | 录入 MES 工单号 |
| POST | `/admin-api/oms/production/sync?orderId=` | 从 MES 同步状态（v1 手工触发）。body `{"mesStatus": "已开料"}`，取 03 篇 §4.3 的车间术语；返回给用户看的进度文案。术语不认识或当前状态推不过去都报错，不会静默不改 |
| GET | `/admin-api/oms/production/mes-status` | 车间术语表（`{术语: {orderStatus, userText}}`），后台下拉框数据源，避免前端写死 MES 口径 |

**发货**

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin-api/oms/delivery` | 录入发货信息 |
| POST | `/admin-api/oms/delivery/batch` | 批量发货（Excel 导入） |
| GET | `/admin-api/oms/delivery/print` | **电子面单打印**（适配器 + 桩） |
| GET | `/admin-api/oms/delivery/track?no=` | 物流轨迹 |
| GET | `/admin-api/oms/delivery/{orderId}` | 按订单查发货信息（订单详情页物流卡片） |

**售后**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/oms/aftersale/page` | 分页 |
| GET | `/admin-api/oms/aftersale/{id}` | 详情 |
| POST | `/admin-api/oms/aftersale/apply` | **客服代客申请售后**（与顾客端同规则：退款窗口、按明细行） |
| ✱ POST | `/admin-api/oms/aftersale/{id}/approve` | 通过 → 触发退款 |
| ✱ POST | `/admin-api/oms/aftersale/{id}/reject` | 驳回 |
| ✱ POST | `/admin-api/oms/order/{id}/force-refund` | **强制退款申请**：body `{"amount": 10.00, "reason": "…"}`，只写一条 `review_status=1` 的退款流水并返回**流水 id**，钱不动、订单状态不动 |
| ✱ POST | `/admin-api/oms/order/force-refund/{refundId}/review` | **财务复核**：body `{"approved": true, "remark": "…"}`，权限 `order:refund:review`。通过后真正出款、订单进 11；驳回则订单状态不变。同一笔重复复核会被拒 |

**发票**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/oms/invoice/page` | 分页 |
| GET | `/admin-api/oms/invoice/{id}` | 详情 |
| POST | `/admin-api/oms/invoice/apply` | **客服代客申请开票** |
| ✱ POST | `/admin-api/oms/invoice/{id}/issue` | 开票 |
| ✱ POST | `/admin-api/oms/invoice/{id}/reject` | 驳回 |
| POST | `/admin-api/oms/invoice/{id}/upload` | 上传电子发票文件 |

### 2.7 审核 `aud`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/aud/image/page` | **待审图片队列**（自动拦截后的剩余） |
| GET | `/admin-api/aud/image/{id}` | 详情（含原图、自动审核结果） |
| POST | `/admin-api/aud/image/check` | 单图送审（pHash 黑名单 + 机器审核，与顾客端上传即审同一实现） |
| ✱ POST | `/admin-api/aud/image/{id}/approve` | 放行 |
| ✱ POST | `/admin-api/aud/image/{id}/reject` | 驳回 |
| POST | `/admin-api/aud/image/batch-audit` | 批量审核 |

**黑名单**

| 方法 | 路径 |
|---|---|
| GET | `/admin-api/aud/blacklist/page` |
| POST | `/admin-api/aud/blacklist` |
| DELETE | `/admin-api/aud/blacklist/{id}` |
| POST | `/admin-api/aud/blacklist/check` | 上传图片查 pHash |

**敏感词**

| 方法 | 路径 |
|---|---|
| GET | `/admin-api/aud/sensitive-word/page` |
| POST/PUT/DELETE | `/admin-api/aud/sensitive-word` |

**版权承诺**

| 方法 | 路径 |
|---|---|
| GET | `/admin-api/aud/promise/page` |

### 2.8 营销 `mkt`

**活动（通用）**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/mkt/activity/page` | 分页（按 type 筛） |
| GET | `/admin-api/mkt/activity/{id}` | 详情（含 rule_config） |
| POST/PUT | `/admin-api/mkt/activity` | 增改 |
| PUT | `/admin-api/mkt/activity/{id}/status` | 启停 |
| DELETE | `/admin-api/mkt/activity/{id}` | 删除 |

**拼团**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/mkt/group/page` | 团列表（状态/到期时间） |
| GET | `/admin-api/mkt/group/{id}` | 详情含团员 |
| GET | `/admin-api/mkt/group/{id}/members` | 团员列表（分页数据源） |
| ✱ POST | `/admin-api/mkt/group/{id}/force-success` | **强制成团**（模拟成团） |
| ✱ POST | `/admin-api/mkt/group/{id}/force-fail` | 强制失败 + 退款 |

**秒杀**

| 方法 | 路径 |
|---|---|
| GET | `/admin-api/mkt/seckill/page` · `GET /{id}` |
| POST/PUT/DELETE | `/admin-api/mkt/seckill` |

> 秒杀活动即 `type = 2` 的活动，此处只保留秒杀特有的**独立库存配额**（`total_stock` / `sold_stock`，见 01 篇 §7）字段管理，其余字段走活动通用接口。

**优惠券**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/mkt/coupon/page` | 券模板分页 |
| POST/PUT/DELETE | `/admin-api/mkt/coupon` | 增改删 |
| ✱ POST | `/admin-api/mkt/coupon/{id}/grant` | **定向发放**（记快照） |
| ✱ POST | `/admin-api/mkt/coupon/{id}/grant-batch` | 批量发放 |
| GET | `/admin-api/mkt/coupon/user/page` | 用户持券与核销记录 |

> 领券中心属顾客端页面，后台 v1 只做发放与核销管理。

**邀请好友**

| 方法 | 路径 |
|---|---|
| GET | `/admin-api/mkt/invite/page` | 邀请记录 |
| GET | `/admin-api/mkt/invite/statistics` | 拉新统计 |

### 2.9 客服 `svc`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/svc/ticket/page` | 工单分页（按类型/状态） |
| GET | `/admin-api/svc/ticket/{id}` | 详情含回复 |
| GET | `/admin-api/svc/ticket/{id}/replies` | 回复列表（工单详情页时间线） |
| POST | `/admin-api/svc/ticket` | **客服代客建单** |
| POST | `/admin-api/svc/ticket/{id}/reply` | 回复 |
| PUT | `/admin-api/svc/ticket/{id}/status` | 改状态（四态流转） |
| PUT | `/admin-api/svc/ticket/{id}/handler` | 分配处理人 |
| POST | `/admin-api/svc/ticket/{id}/close` | 关闭 |

**侵权投诉**走同一工单表，`type = 2`，处置结论记在回复里。

### 2.10 财务与看板 `fin`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/fin/dashboard/overview` | 经营看板：今日/本月销售额、订单数、品类排行 |
| GET | `/admin-api/fin/dashboard/todo` | **运营待办**：待处理订单数、待审图数、待发货数、超时告警 |
| GET | `/admin-api/fin/dashboard/trend` | 销售趋势（按日） |
| GET | `/admin-api/fin/settlement/page` | 日结列表 |
| GET | `/admin-api/fin/settlement/reconcile` | **对账**（与微信支付流水核对） |
| GET | `/admin-api/fin/refund/page` | 退款流水 |
| GET | `/admin-api/fin/refund/export` | 导出 |

**分三页的理由**：把老板看的销售数据和客服每天要盯的待办塞一页，两边都不好用。**「待办」才是客服每天真正要盯的指标。**

### 2.11 文件 `file`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/admin-api/file/upload` | 上传（走 `FileStorage` 适配器，v1 本地实现） |
| GET | `/admin-api/file/download?key=` | 下载 |

### 2.12 延迟任务 `tsk`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin-api/tsk/delay-log/page` | 延迟任务执行日志 |
| POST | `/admin-api/tsk/delay-log/{id}/retry` | **手动重试失败任务** |
| POST | `/admin-api/tsk/scan/trigger` | **手动触发兜底扫描**（排查用） |

### 2.13 顾客端 `client`

> 小程序演示版所需接口。与后台接口**同源同规则、不同视角与权限**。调用方为**小程序用户**，只能操作自己的数据。前缀 `/client-api/{module}/{action}`，响应体与错误码与后台一致（见 §1）。

**认证**

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/client-api/auth/wx-login` | `wx.login` 的 `code` 换 token（v1 桩：任意 code 换固定 token + 种子用户） |

**商品（只读）**

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/client-api/pms/series/list` | 系列列表 |
| GET | `/client-api/pms/category/tree` | 品类树 |
| GET | `/client-api/pms/spu/page` | 商品分页 |
| GET | `/client-api/pms/spu/{id}` | 商品详情（含 SKU / 工艺 / 计价方式） |
| GET | `/client-api/pms/spu/{id}/skus` | 商品的 SKU 列表（规格选择器） |

**文件与审核**

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/client-api/file/upload` | 上传（复用 `FileStorage`，v1 本地实现） |
| POST | `/client-api/aud/image/check` | 上传即审：pHash 黑名单比对 + 机器审核（v1 桩恒通过），返回 `through / suspect / reject` |

**订单**

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/client-api/oms/order/preview` | 计价预览（按 `pricing_type` 算商品金额 + 运费，不落库）。支持 `items[]` 混单（03 篇 §1.3）：逐行计价、金额求和、运费按整单；响应在整单金额之外给 `lines[]` 逐行小计（skuId / 商品名 / 数量 / 小计） |
| POST | `/client-api/oms/order/submit` | 提交订单（含图槽位、版权承诺版本、`activityId`/`couponUserId`/`groupId`/`shareCode`）。**落库为 1 待付款，支付由 `POST /client-api/oms/order/{id}/pay` 显式触发**（支付桩），不在 submit 里内联扣款。支持 `items[]` 混单：传了以它为准（每行 `{skuId, qty, craftIds, activityId, images[]}`，上限 20 行），不传则按单行字段组装一行 |
| ✱ POST | `/client-api/oms/order/seckill` | **秒杀下单（削峰入口，03 篇 §2.6）**：只受理、不落单，立即返回 `{requestId, status:"QUEUED"}`。入参与 `submit` 同构。仅秒杀活动单走此入口，普通下单仍走 `submit`。该路径默认被限流（`orangecrave.rate-limit.rules`，键为精确路径），令牌耗尽回 `code=429` |
| GET | `/client-api/oms/order/seckill/result/{requestId}` | 轮询受理结果：`QUEUED` 继续轮询 / `SUCCESS`（带 `orderId`）/ `FAILED`（带可展示 `message`）。`data` 为 `null` 表示 requestId 不存在或结果已过期（保留 10 分钟） |
| GET | `/client-api/oms/order/mine` | 我的订单（分页） |
| GET | `/client-api/oms/order/{id}` | 订单详情（本人的订单，越权访问返回 404 语义） |
| GET | `/client-api/oms/order/{id}/timeline` | 状态流转时间线（与 §2.6 同一实现） |

**售后与渠道**

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/client-api/oms/aftersale/apply` | 申请售后（退款窗口校验：付款后 4 小时内） |
| POST | `/client-api/channel/visit` | 渠道上报（如 `channelCode: 'xhs'`） |

> **与后台接口的关系**：同一套业务规则与数据模型，仅在（a）调用方身份、（b）可见字段（如身份证号脱敏）、（c）可执行动作（用户只能操作自己的资源）三点不同。实现上在对应模块下以 `client/` 分组承载，**不复制业务逻辑**。

---

## 3. 错误码约定

| 码段 | 模块 |
|---|---|
| 1001–1099 | 通用校验（参数、状态冲突） |
| 2001–2099 | 订单（不可取消、不可退款、已发货…） |
| 2101–2199 | 售后（超出售后窗口…） |
| 2201–2299 | 拼团（团已满、团已失效…） |
| 3001–3099 | 审核（图片违规、命中黑名单…） |
| 4001–4099 | 商品（已下架、库存不足…） |
| 5001–5099 | 分享码（失效、超量、作品封禁…） |

**分享码失效原因文案（本工程自拟，不照抄他方产品）**：

```
分享码已超过有效期
关联作品已封禁
分享码已达下单上限
```

---

## 4. 需与顾客端风格统一的三点

| 项 | 顾客端 | 后台 v1 |
|---|---|---|
| 认证头 | `Authorization: Bearer <token>` | **同左** |
| 响应体 | `{code, msg, data}`，`0` 成功 | **同左** |
| 401 语义 | 重新登录 | **同左** |

**不同**：顾客端另有 `code=1000`（刷新 token），后台 v1 保留该值不占用。
