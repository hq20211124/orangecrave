# 07 · 小程序端配合清单

> **前提**：本项目**拿不到**橙刻原有小程序源码，因此小程序端按「**新建最小演示版**」处理，不复刻原 83 个页面。

## 1. 定位

| 项 | 说明 |
|---|---|
| 目标 | 跑通主链路，验证后台接口可用 |
| 不复刻 | 原版 83 页 / 9 分包 |
| AppID | **另行注册**（用户提供） |
| 后端 | 指向**本项目自建后台** |

### 1.1 为什么不能用原 AppID

原 AppID 的服务端会**校验 appid 白名单**，新 AppID 调用原接口会被拒。因此演示版必须连自建后端——这与「独立系统、本机测试」的定位一致。

### 1.2 为什么不复刻全部页面

原版含大量强耦合能力（画布编辑器、3D 预览、WASM 渲染模块、拼版），复刻成本极高且无验证价值。**主链路跑通**才能证明后台设计可用。

---

## 2. 演示版页面清单

**只做这一条链路**：

```
首页 → 选品类 → 商品列表 → 商品详情 → 定制编辑（传图）
                                              │
                                              ↓
                       我的订单 ← 支付 ← 确认下单
                          │
                          ├─ 订单详情（含生产进度）
                          └─ 申请售后
```

| # | 页面 | 对应后台接口 |
|---|---|---|
| 1 | 首页 | `GET /pms/series/list`、`GET /pms/category/tree` |
| 2 | 商品列表 | `GET /pms/spu/page` |
| 3 | 商品详情 | `GET /pms/spu/{id}` |
| 4 | **定制编辑（传图）** | `POST /file/upload`、`POST /aud/image/check` |
| 5 | 确认订单 | `POST /oms/order/preview`（计价预览） |
| 6 | 支付 | `POST /oms/order/submit` |

> 确认页 v1 仍是**单 SKU 单行**流程，用单行简写字段提交；后台已支持 `items[]` 混单（03 篇 §1.3），小程序后续做购物车时直接传 `items` 即可，接口无需再改。| 7 | 订单列表 | `GET /oms/order/mine` |
| 8 | **订单详情（生产进度）** | `GET /oms/order/{id}` + `GET /oms/order/{id}/timeline` |
| 9 | 申请售后 | `POST /oms/aftersale/apply` |
| 10 | 我的 | 用户信息 |

**共 10 页**，对比原版 83 页。

### 2.1 不做

| 不做 | 原因 |
|---|---|
| 画布编辑器 / 3D 预览 / WASM 渲染 | 原版有服务端图像能力，复刻成本极高 |
| 拼团 / 秒杀 | 后台支持，但演示版不验证 |
| 作品分享 / 分享码 / 无料分享 | 同上，链路复杂 |
| 购物车 | 单品直购即可验证主链路 |

---

## 3. 必须与后台对齐的三项

### 3.1 认证

```js
wx.login() → code
  ↓
POST /api/auth/wx-login  { code }
  ↓
{ code: 0, data: { token } }
  ↓
后续请求头：Authorization: Bearer <token>
```

与顾客端风格一致。

### 3.2 响应信封

```json
{ "code": 0, "msg": "success", "data": {} }
```

`code === 0` 成功，`401` 触发重新登录。

### 3.3 订单状态枚举

**这是本清单最重要的一项。**

演示版小程序必须理解后台的**完整状态**——因为自印业务的订单要经过生产环节：

```js
const ORDER_STATUS = {
  0:  { text: '待付款',   color: '#FF9500' },
  1:  { text: '待成团',   color: '#FF9500' },
  2:  { text: '待审核',   color: '#007AFF' },
  3:  { text: '待生产',   color: '#007AFF' },
  4:  { text: '生产中',   color: '#007AFF' },   // ★ 后台新增
  5:  { text: '质检中',   color: '#007AFF' },   // ★ 后台新增
  6:  { text: '待发货',   color: '#007AFF' },
  7:  { text: '已发货',   color: '#34C759' },
  8:  { text: '已完成',   color: '#8E8E93' },
  9:  { text: '已取消',   color: '#8E8E93' },
  10: { text: '售后中',   color: '#FF9500' },
  11: { text: '已退款',   color: '#8E8E93' }
};
```

> **★ 标记的两个状态是后台特有的**：原顾客端只区分「待发货 / 已发货 / 已完成」，而自印业务中用户最焦虑的恰恰是「我的卡做到哪了」。
>

### 3.4 生产进度的展示

订单详情页需展示后台返回的**粗粒度进度**（详见 `03-order-production.md` 4.3）：

```
已下单 → 审核通过 → 制作中 → 质检中 → 已发货 → 已完成
```

MES 的细粒度术语（「已上机裱纸」）**不透传给用户**，由后台映射为可读文案。

---

## 4. 传给后台的关键数据

### 4.1 下单时的图槽位

**一个明细可含多张图，每张图有独立份数**：

```json
{
  "skuId": 123,
  "items": [
    { "slotIndex": 0, "imageUrl": "https://...", "copies": 1 },
    { "slotIndex": 1, "imageUrl": "https://...", "copies": 1 }
  ]
}
```

覆盖两种真实需求：

- 20 张不同的图（套卡） → 20 个槽位，各 `copies: 1`
- 1 张图印 20 份（送人） → 1 个槽位，`copies: 20`

**这个结构必须在演示版就做对**——若先按任一种建模，另一种需求来了要改表。

### 4.2 版权承诺

上传前**必须**让用户勾选版权承诺，并把 `promiseVersion` 传给后台：

```json
{ "imageUrl": "...", "promiseVersion": "v1.0" }
```

后台每次记录一条 `aud_copyright_promise`（含版本与 IP）。

### 4.3 渠道来源

首次进入时上报来源，后续下单携带 `channelId`：

```js
// 参照原版：shop/xhs/visit 用于小红书渠道归因
POST /api/channel/visit { channelCode: 'xhs' }
```

---

## 5. 开发环境配置

```js
// config.js
const ENV = {
  dev:  { apiBase: 'http://localhost:8080/api' },   // 本地后台
  test: { apiBase: 'http://<测试机>:8080/api' }
};
```

**AppID**：由用户注册后填入 `project.config.json`。

**不配第三方域名**——演示版只连自建后台。

---

## 6. 本清单不含

| 项 | 原因 |
|---|---|
| 小程序 UI 设计 | 本项目只做后台设计 |
| 原版页面迁移 | 无源码，且不值得 |
| 生产环境部署 | 演示版定位 |
| 微信支付真实对接 | 走桩实现 |

---

## 7. 与后台的接口契约

演示版需要的接口，**后台侧已全部设计**（见 `02-api.md`）：

| 演示版需要 | 后台接口 | 后台文档 |
|---|---|---|
| 登录 | `POST /client-api/auth/wx-login` | 02 §2.13 |
| 品类 / 系列 | `GET /client-api/pms/series/list` | 02 §2.13 |
| 商品列表 / 详情 | `GET /client-api/pms/spu/page`、`/{id}` | 02 §2.13 |
| 传图 | `POST /client-api/file/upload` | 02 §2.13 |
| 图片审核 | `POST /client-api/aud/image/check` | 02 §2.13 |
| 计价预览 | `POST /client-api/oms/order/preview` | 02 §2.13 |
| 提交订单 | `POST /client-api/oms/order/submit` | 02 §2.13 |
| 我的订单 | `GET /client-api/oms/order/mine` | 02 §2.13 |
| 订单详情 + 时间线 | `GET /client-api/oms/order/{id}/timeline` | 02 §2.13 |
| 申请售后 | `POST /client-api/oms/aftersale/apply` | 02 §2.13 |

> 顾客端接口已补入 `02-api.md` §2.13（`/client-api/` 前缀）。它们与后台接口是**同一套业务规则与数据模型**，只是调用方身份、可见字段、可执行动作不同；实现以各模块下的 `client/` 分组承载，**不复制业务逻辑**。

---

## 8. 一句话总结

小程序端需要配合的**只有三件事**：

1. **换 AppID + 指向自建后台**
2. **理解完整的订单状态枚举**（尤其是生产环节那几个）
3. **下单时传对图槽位结构**

其余都是后台的事。
