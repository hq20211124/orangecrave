# orangecrave-miniprogram

橙刻定制 · **小程序最小演示版**（见设计 `07-miniprogram-changes.md`）。

只跑通主链路，**不复刻**原版 83 页：

```
首页 → 商品列表 → 商品详情 → 定制编辑（传图）→ 确认下单
                                                    ↓
                   我的订单 ← 支付（演示桩）← ───────┘
                      ├─ 订单详情（含生产进度时间线）
                      └─ 申请售后
```

共 8 页：`index` / `goods/list` / `goods/detail` / `order/confirm` / `order/list` / `order/detail` / `aftersale/apply` / `my`。

## 运行

1. **另注册 AppID**（原 AppID 的服务端会校验白名单，无法调用自建后端）
2. 填入 `project.config.json` 的 `appid`
3. 启动后台（见 [`../orangecrave-admin/README.md`](../orangecrave-admin/README.md)），确认 `http://localhost:8080` 可访问
4. 用微信开发者工具打开本目录；本地调试需勾选「不校验合法域名」

## 对齐后台的三项（07 篇 §3）

| 项 | 实现位置 |
|---|---|
| 认证 `Authorization: Bearer <token>` | `utils/request.js` |
| 统一响应信封 `{code,msg,data}`，401 重新登录 | `utils/request.js` |
| **完整订单状态枚举**（含后台特有的 生产中/质检中） | `utils/config.js` |

## 关键数据

- 下单传**图槽位结构**（见 07 篇 §4.1）：一个明细下可挂多个「图-份数」对，本演示版传 1 个槽位 × N 份
- 上传前**版权承诺**：`promiseVersion` 随传图一起提交，后台落 `aud_copyright_promise`
- 渠道来源：启动时上报 `channelCode: 'xhs'`

> 支付为**演示桩**（`POST /client-api/oms/order/{id}/pay`），线上应走 `wx.requestPayment`。
