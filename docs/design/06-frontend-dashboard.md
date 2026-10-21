# 06 · 前端与看板

## 1. 技术选择

**原生 HTML / JS / CSS，不用框架**，由 Spring Boot 作为静态资源托管。

| 项 | 选择 |
|---|---|
| 位置 | `orangecrave-admin-web/src/main/resources/static/admin/` |
| 路由 | 多页应用（MPA），每页一个 HTML |
| 请求 | 原生 `fetch` |
| 样式 | 手写 CSS，一套共享变量与组件类 |
| 依赖 | **零第三方依赖**（不引 jQuery / Layui / Element） |

**为什么用 MPA 而非 SPA**：不用框架时，SPA 的路由、状态管理、组件化都要自己造，成本高于收益。多页应用每页独立、刷新即重置、调试直观——对「本机演示 + 内部工具」这个定位更合适。

---

## 2. 目录结构

```
static/admin/
├─ index.html              登录页
├─ layout.html             主框架（左侧菜单 + 顶栏 + 内容 iframe）
├─ css/
│   ├─ variables.css       颜色/间距/字号变量
│   ├─ base.css            重置与基础排版
│   ├─ components.css      按钮/表格/表单/弹窗/标签
│   └─ page.css            页面级样式
├─ js/
│   ├─ config.js           API 前缀、常量
│   ├─ request.js          ★ 统一请求封装
│   ├─ auth.js             ★ token 管理、权限判断
│   ├─ table.js            ★ 表格 + 分页渲染
│   ├─ form.js             表单序列化与校验
│   ├─ dialog.js           弹窗 / 确认框 / 提示
│   └─ util.js             日期格式化、金额格式化
└─ pages/                  各业务页面
    ├─ order/ · goods/ · work/ · audit/ · marketing/
    ├─ service/ · finance/ · system/ · user/
```

---

## 3. 共享 JS 设计

### 3.1 `request.js` —— 统一请求

**必须与后台接口规范严格对应**（见 `02-api.md`）：

```js
async function request(url, options = {}) {
  const token = sessionStorage.getItem('token');
  const res = await fetch(API_PREFIX + url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : '',
      ...options.headers
    }
  });
  const body = await res.json();

  if (body.code === 401) {            // 与顾客端语义一致
    sessionStorage.clear();
    location.href = '/admin/index.html';
    throw new Error('未登录');
  }
  if (body.code !== 0) {
    Dialog.toast(body.msg);
    throw new Error(body.msg);
  }
  return body.data;
}

const http = {
  get:  (url, params) => request(url + qs(params)),
  post: (url, data)   => request(url, { method: 'POST', body: JSON.stringify(data) }),
  put:  (url, data)   => request(url, { method: 'PUT',  body: JSON.stringify(data) }),
  del:  (url)         => request(url, { method: 'DELETE' })
};
```

**同时携带幂等键**（写操作）：

```js
'Idempotency-Key': crypto.randomUUID()
```

### 3.2 `auth.js` —— 权限

后端返回权限标识列表，前端据此**显示/隐藏按钮**：

```js
function hasPerm(key) {
  return JSON.parse(sessionStorage.getItem('perms') || '[]').includes(key);
}

// 用法：隐藏无权限的按钮
document.querySelectorAll('[data-perm]').forEach(el => {
  if (!hasPerm(el.dataset.perm)) el.remove();
});
```

> **前端隐藏只是体验优化，不是安全边界**。服务端必须**独立校验**每一个敏感操作——前端能改，服务端不能信。

### 3.3 `table.js` —— 表格与分页

```js
Table.render({
  el: '#orderTable',
  url: '/admin-api/oms/order/page',
  columns: [
    { field: 'orderNo', title: '订单号' },
    { field: 'status',  title: '状态', render: renderOrderStatus },
    { field: 'payAmount', title: '实付', render: money },
    { title: '操作', render: (row) => `
        <a href="detail.html?id=${row.id}">详情</a>
        ${hasPerm('order:refund:force')
          ? `<button class="btn-danger" data-perm="order:refund:force"
                     onclick="forceRefund(${row.id})">强制退款</button>`
          : ''}
    `}
  ],
  query: () => Form.serialize('#searchForm')
});
```

### 3.4 `dialog.js` —— 弹窗

```js
Dialog.confirm('确认强制退款？此操作会记录操作日志。').then(ok => { ... });
Dialog.toast('操作成功');
Dialog.open({ title: '详情', url: 'detail.html?id=1', width: 900 });
```

**敏感操作的确认框必须写明后果**（如「此操作会记入操作日志」）。

---

## 4. 页面清单

> 菜单结构。括号内为权限标识前缀。

### 4.1 首页

| 页面 | 文件 | 说明 |
|---|---|---|
| **运营待办** | `pages/home/todo.html` | **登录后首页** —— 每天要处理的事 |

### 4.2 订单管理（`oms:`）

| 页面 | 说明 |
|---|---|
| 订单列表 | 多条件筛选、批量导出 |
| 订单详情 | 明细、图槽位、**状态流转时间线** |
| **生产工单** | 生产队列、推进状态、录 MES 工单号 |
| 发货管理 | 录入运单、**批量导入**、打面单 |
| 售后管理 | 售后单列表与审核 |
| **发票管理** | 开票申请、审核、记录 |

### 4.3 作品与分享（`wms:`）

| 页面 | 说明 |
|---|---|
| 作品列表 | 按状态筛（待审核/待修改/违规/过期） |
| 作品详情 | 图槽位、审核记录、分享状态 |
| 合集管理 | |
| **分享码管理** | 含**预付款核销情况**、强制失效、退未核销余额 |
| 打样单 | `order_type=2` 的订单 |

### 4.4 审核中心（`audit:`）

| 页面 | 说明 |
|---|---|
| **待审队列** | 图片审核主界面（大图 + 快捷通过/驳回） |
| 审核记录 | 历史结论与操作人 |
| 黑名单库 | pHash 库管理 |
| 敏感词 | |
| 版权承诺 | 承诺记录查询 |

**待审队列的交互设计要点**：键盘快捷键（`J`/`K` 切换，`A` 通过，`R` 驳回），审核量大时鼠标点击太慢。

### 4.5 商品管理（`goods:`）

| 页面 | 说明 |
|---|---|
| 系列管理 | |
| 品类管理 | 树形 |
| 商品列表 | |
| 商品编辑 | SPU + 规格 + SKU + 工艺 + **计价方式** |
| 工艺管理 | |
| 运费模板 | |
| 画师管理 | 含销量统计（**只统计不分成**） |
| 模板库 | |

### 4.6 营销管理（`marketing:`）

| 页面 | 说明 |
|---|---|
| 活动列表 | 按类型筛（拼团/秒杀/优惠券/邀请） |
| 活动编辑 | 通用表单 + **动态规则区**（按类型渲染） |
| **拼团管理** | 团列表、强制成团/失败 |
| 秒杀管理 | |
| 优惠券管理 | 券模板、发放、核销记录 |
| 邀请记录 | 拉新统计 |

### 4.7 客服工单（`svc:`）

| 页面 | 说明 |
|---|---|
| 工单列表 | 按类型/状态筛 |
| 工单详情 | 会话式回复、关联订单、处置结论 |

### 4.8 财务与看板（`fin:`）

| 页面 | 说明 |
|---|---|
| **经营看板** | 销售数据 |
| **财务对账** | 流水与对账 |
| 退款流水 | |

### 4.9 系统管理（`system:`）

| 页面 | 说明 |
|---|---|
| 管理员 | |
| 角色权限 | 菜单树 + **按钮级**勾选 |
| 菜单管理 | |
| **系统参数配置** | KV 编辑 |
| 操作日志 | **含改前/改后快照对比** |
| 登录日志 | |
| **延迟任务日志** | 执行记录、失败重试 |

### 4.10 用户管理（`ums:`）

| 页面 | 说明 |
|---|---|
| 用户列表 | 含渠道来源 |
| 用户详情 | 订单、消费额 |
| **身份认证审核** | 团长/摊主认证（通过后自动发打样券） |
| 渠道管理 | |

---

## 5. 看板与对账

### 5.1 分三页的理由

把老板看的销售数据和客服每天要盯的待办塞一页，两边都不好用：

| 页面 | 受众 | 频率 |
|---|---|---|
| **运营待办** | 客服 / 仓管 | **每天多次** |
| **经营看板** | 老板 / 运营 | 每天一次 |
| **财务对账** | 财务 | 每天一次 |

### 5.2 运营待办（首页）

**这是登录后的第一屏**，因为它是每天真正要处理的事：

| 指标 | 含义 | 点击跳转 |
|---|---|---|
| 待付款订单 | 未支付 | → 订单列表 |
| **待审核图片** | 待审队列积压 | → 审核中心 |
| **待生产订单** | 积压 | → 生产工单 |
| **待发货订单** | 积压 | → 发货管理 |
| **待处理工单** | 客服未响应 | → 工单列表 |
| 待处理售后 | | → 售后管理 |
| **超时未发货** | 超过 72 小时 | → 订单列表（筛超时） |
| 拼团即将到期 | 24h 内到期的团 | → 拼团管理 |
| **失败延迟任务** | 需人工重试 | → 延迟任务日志 |

**每项都带数字 + 跳转**。空态显示「今日无待办」。

### 5.3 经营看板

| 指标 | 口径 |
|---|---|
| 今日销售额 | **支付时间**为今日的 `pay_amount` 之和（排除打样单 `order_type≠2`） |
| 本月销售额 | 同上，按月 |
| 今日订单数 | |
| 客单价 | 销售额 / 订单数 |
| **品类销售排行** | 按品类聚合 |
| **系列销售排行** | 按系列聚合 |
| 拼团成团率 | 已成团 / (已成团 + 已失败) |
| 退款率 | 退款额 / 销售额 |
| 销售趋势 | 按日折线图 |

### 5.4 财务对账

**对账口径**【已裁定】：

| 订单类型 | 成交日期 |
|---|---|
| 普通订单 | **支付时间** `pay_time` |
| 拼团订单 | **成团时间** |

**理由**：普通单按支付时间可与微信支付 T+1 结算流水逐笔核；拼团单在成团前那笔钱是「待定」状态，用支付时间会产生「卖了又退」的虚账。

**页面内容**：

| 区域 | 说明 |
|---|---|
| 日结汇总 | 日期、销售额、退款额、净收入、订单数 |
| **对账明细** | 期望值（系统）vs 实际值（微信支付流水）vs **差异** |
| 差异标红 | 有差异的行高亮，支持导出 |
| 退款流水 | 独立列出，**不冲减销售额** |

**退款必须记独立流水**（`fin_refund_ledger`）的理由：若直接冲减销售额，则「卖了 100 退了 100」与「没卖」在账上无法区分，而前者说明有质量问题需要追查。

---

## 6. 前端约定

| 项 | 约定 |
|---|---|
| 金额显示 | 两位小数，千分位 |
| 时间显示 | `yyyy-MM-dd HH:mm:ss` |
| 长文本 | 超长截断 + 悬停显示全文 |
| 危险操作 | 红色按钮 + **二次确认** |
| 敏感操作确认 | 文案必须写明后果 |
| 表单校验 | 前端校验 + **服务端必再校验** |
| 空态 | 明确的空态提示，不留白 |
| 加载中 | 按钮禁用 + loading，防重复提交 |

### 前端三处必须做到

1. **幂等**：写操作带 `Idempotency-Key`，防重复提交
2. **权限**：按 `data-perm` 隐藏按钮，但**不依赖它做安全**
3. **敏感确认**：强制退款、改价、审核的确认框写明后果
