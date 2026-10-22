# 01 · 数据库设计

MySQL 8.0，utf8mb4。所有表使用 InnoDB。

## 0. 通用约定

**公共字段**（下文不再重复列出）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint unsigned auto_increment | 主键 |
| `create_time` | datetime | 创建时间 |
| `update_time` | datetime | 更新时间 |
| `deleted` | tinyint | 逻辑删除，0 正常 / 1 删除 |

**金额**：统一 `decimal(12,2)`，单位元。**不使用 float/double**。
**库存与数量**：`int`。
**状态**：`tinyint`，取值在注释中枚举，并与代码常量类一一对应。

**命名**：`{模块前缀}_{实体}`，模块前缀：`sys_` `ums_` `pms_` `wms_` `oms_` `aud_` `mkt_` `svc_` `fin_` `tsk_`。

**索引约定**：外键列建普通索引；状态 + 时间类查询建联合索引；订单号、分享码等业务唯一键建唯一索引。

---

## 1. 系统管理

### sys_user 管理员

| 字段 | 类型 | 说明 |
|---|---|---|
| `username` | varchar(50) | 登录名，唯一 |
| `password` | varchar(100) | BCrypt 哈希 |
| `nickname` | varchar(50) | 显示名 |
| `phone` | varchar(20) | 手机 |
| `status` | tinyint | 0 停用 / 1 正常 |
| `last_login_time` | datetime | |
| `last_login_ip` | varchar(50) | |

### sys_role 角色

| 字段 | 类型 | 说明 |
|---|---|---|
| `role_name` | varchar(50) | |
| `role_key` | varchar(50) | 唯一标识，如 `customer_service` |
| `remark` | varchar(255) | |
| `status` | tinyint | 0 停用 / 1 正常 |

### sys_menu 菜单与按钮权限

| 字段 | 类型 | 说明 |
|---|---|---|
| `parent_id` | bigint | 父菜单，0 为根 |
| `menu_name` | varchar(50) | |
| `menu_type` | tinyint | 1 目录 / 2 菜单 / 3 **按钮** |
| `path` | varchar(200) | 前端路由 |
| `perm_key` | varchar(100) | 权限标识，如 `order:refund:force` |
| `icon` | varchar(100) | |
| `sort` | int | |

> 按钮级权限即 `menu_type = 3` 的记录，`perm_key` 用于 Sa-Token 的 `@SaCheckPermission`。

### sys_user_role / sys_role_menu

标准多对多关联表，各含 `user_id`/`role_id`、`role_id`/`menu_id`。

### sys_oper_log 操作日志

| 字段 | 类型 | 说明 |
|---|---|---|
| `oper_module` | varchar(50) | 模块 |
| `oper_type` | varchar(20) | INSERT / UPDATE / DELETE / **FORCE_REFUND** 等 |
| `oper_desc` | varchar(255) | 描述 |
| `method` | varchar(200) | 类.方法 |
| `request_url` | varchar(255) | |
| `request_param` | text | |
| `**before_data**` | json | **改前快照** |
| `**after_data**` | json | **改后快照** |
| `oper_user_id` / `oper_user_name` | | |
| `oper_ip` / `oper_time` | | |
| `status` | tinyint | 0 失败 / 1 成功 |
| `error_msg` | text | |

> **改前/改后快照是刻意的设计**：系统里有版权审核与退款两条敏感线，出事时要能回答「这张图是谁放行的」「这个价原来是多少」——只记动作不记内容，事后查不出来。

**必须记快照的操作**（`oper_type` 白名单）：登录、改价、强制退款、审核放行/驳回、手工改订单状态、发券、改系统配置。

### sys_login_log

`user_name` / `ip` / `browser` / `os` / `status` / `msg` / `login_time`。

### sys_config 系统参数配置

| 字段 | 类型 | 说明 |
|---|---|---|
| `config_key` | varchar(100) | 唯一，如 `freeShareConfig` |
| `config_value` | text | |
| `config_type` | tinyint | 0 内置 / 1 可改 |
| `remark` | varchar(255) | |

> 顾客端已有同名机制（`sys/selectParamsConfigByKey`，key 如 `freeShareConfig` / `enableMyWork` / `batchCustom`）。后台做成可视化编辑页。

**必须落库的参数**：待付款超时分钟数、拼团默认人数、拼团默认时长、退款窗口小时数、自动确认收货天数、分享码默认有效期、无料分享起订量规则、模拟成团开关、满额包邮阈值、基础邮费。

### sys_idempotency 幂等记录

| 字段 | 类型 | 说明 |
|---|---|---|
| `idempotency_key` | varchar(128) | 唯一，客户端每次写操作生成的 UUID |
| `request_method` | varchar(10) | HTTP 方法 |
| `request_path` | varchar(255) | 请求路径 |
| `request_body` | text | 请求体（用于比对同 key 不同体） |
| `response_body` | text | 首次响应（重放时直接返回） |
| `status` | tinyint | 0 处理中 / 1 已完成 / 2 失败 |
| `expire_time` | datetime | 过期时间，超时后可物理删除 |
| `create_time` / `update_time` | datetime | |

**索引**：`uk_key (idempotency_key)` 唯一键、`idx_create_time (create_time)` 用于 TTL 清理。

**清理**：`IdempotencyCleanupTask` 定时物理删除超过 24 小时的记录（逻辑删除会占唯一键导致同 key 重放失败）。

---

## 2. 用户

### ums_user 小程序用户

| 字段 | 类型 | 说明 |
|---|---|---|
| `openid` | varchar(64) | 唯一 |
| `unionid` | varchar(64) | |
| `nickname` / `avatar` | | 微信授权信息 |
| `phone` | varchar(20) | |
| `identity_type` | tinyint | 0 普通 / 1 **团长** / 2 **摊主** |
| `cert_status` | tinyint | 0 未提交 / 1 待审 / 2 通过 / 3 驳回 |
| `channel_id` | bigint | **渠道来源**，关联 sys_channel |
| `vip_level` | tinyint | **v1 不使用，仅留字段位** |
| `points` | int | **v1 不使用，仅留字段位** |

### ums_identity_cert 身份认证记录

| 字段 | 类型 | 说明 |
|---|---|---|
| `user_id` | bigint | |
| `identity_type` | tinyint | 1 团长 / 2 摊主 |
| `real_name` | varchar(50) | |
| `id_card_no` | varchar(50) | **加密存储** |
| `material_urls` | json | 证明材料图片 |
| `status` | tinyint | 1 待审 / 2 通过 / 3 驳回 |
| `audit_user_id` / `audit_time` / `audit_remark` | | |
| `proof_coupon_granted` | tinyint | **是否已发打样券** |

### ums_address 收货地址

`user_id` / `receiver_name` / `receiver_phone` / `province` / `city` / `district` / `detail` / `is_default`。

> **注意**：订单**不引用此表**，只存快照（见 §5.3）。

### sys_channel 渠道

`channel_name`（如「小红书」「自然流量」）/ `channel_code` / `status`。

> `shop/xhs/visit`、`shop/xhs/order` 端点存在，说明有小红书流量与订单归因。

---

## 3. 商品

### 层级

```
pms_series（系列/IP主题）→ pms_category（品类）→ pms_spu（商品）→ pms_sku（规格）
```

### pms_series 系列

| 字段 | 类型 | 说明 |
|---|---|---|
| `series_name` | varchar(100) | 如「某番联名」 |
| `cover` | varchar(255) | |
| `ip_name` | varchar(100) | IP 名称 |
| `sort` / `status` | | |

> 加这一层的原因：联名与 IP 主题是核心卖点，「千款联名」若平铺成一堆商品，运营无法按系列上下架、也无法按系列统计销量。

### pms_category 品类

`category_name` / `parent_id` / `sort` / `status`。

例：小卡 / 卡册 / 手机壳 / 卡套 / 笔记本 / 亚克力。

### pms_spu 商品

| 字段 | 类型 | 说明 |
|---|---|---|
| `spu_name` / `sub_title` / `cover` / `images` | | |
| `series_id` / `category_id` | bigint | |
| `goods_type` | tinyint | 1 **定制** / 2 **现货** |
| `**pricing_type**` | tinyint | **1 按张 / 2 按面积 / 3 数量阶梯** |
| `base_price` | decimal(12,2) | 基础单价 |
| `area_price` | decimal(12,2) | 每平米单价（按面积时用） |
| `designer_id` | bigint | 画师，可空 |
| `min_order_qty` | int | **起订量** |
| `increment_qty` | int | **递增量** |
| `freight_template_id` | bigint | 运费模板 |
| `need_proof` | tinyint | **是否需打样才能分享** |
| `status` | tinyint | 0 下架 / 1 上架 |

> **`pricing_type` 是本节最关键的字段**：小卡按张、亚克力相框按面积，硬统一成一种算法必有一类算错。三种并存，商品维度可选。

### pms_spec_group / pms_spec_value 规格

`pms_spec_group`：`spu_id` / `group_name`（如「尺寸」「材质」）/ `sort`
`pms_spec_value`：`group_id` / `value_name`（如「86×54mm」「亚克力」）/ `sort`

### pms_sku 规格组合

| 字段 | 类型 | 说明 |
|---|---|---|
| `spu_id` | bigint | |
| `sku_code` | varchar(64) | 唯一 |
| `spec_value_ids` | json | 规格值组合 |
| `price` | decimal(12,2) | |
| `stock` | int | **现货用**；定制品不占库存 |
| `weight` | decimal(10,2) | 计运费 |
| `image` | varchar(255) | |
| `status` | tinyint | |

### pms_craft 工艺

`craft_name`（白墨 / 烫金 / 覆膜 / 板材 …）/ `craft_type` / `add_price` / `add_price_type`（固定 / 按面积）/ `status`。

`pms_sku_craft`：`sku_id` / `craft_id` 关联。

> 工艺从 SKU 带出，进入生产工单的工序清单。

### pms_freight_template 运费模板

| 字段 | 类型 | 说明 |
|---|---|---|
| `template_name` | varchar(100) | |
| `charge_type` | tinyint | 1 按件 / 2 按重 / 3 按地区 |
| `first_unit` / `first_fee` | | 首件/首重费用 |
| `extra_unit` / `extra_fee` | | 续件/续重费用 |
| `free_threshold` | decimal(12,2) | **满额包邮阈值** |
| `region_rules` | json | 偏远地区加价 |

> 现有规则：基础邮费 4 元/单，**满 28 元减 4 元运费**。默认模板照此配置。

### pms_designer 画师

`designer_name` / `avatar` / `intro` / `status`。

> **只做归属不做分成**。理由：分成规则各家合同不同（按售价百分比 / 按张 / 买断），硬编进系统既不通用又易算错。v1 只统计「某画师作品卖了多少」，出账线下用 Excel。

### pms_template 画师模板库

`template_name` / `designer_id` / `category_id` / `preview_url` / `source_url` / `status`。

---

## 4. 作品与分享

> 这是本设计中**最容易被低估**的一块。作品不是「一张图」，它是有独立状态机的一等实体，并衍生出分享码、打样、无料分享、合集四条支线。

### wms_work 作品

| 字段 | 类型 | 说明 |
|---|---|---|
| `user_id` | bigint | 作者 |
| `work_name` | varchar(100) |  |
| `cover_url` | varchar(255) | |
| `spu_id` / `sku_id` | bigint | 所属商品规格 |
| `**status**` | tinyint | **见下方状态枚举** |
| `audit_remark` | varchar(255) | 审核意见 |
| `expire_time` | datetime | 过期时间 |
| `source_share_id` | bigint | 若来自他人分享 |
| `deleted_shared` | tinyint | 分享中不可删除 |

**作品状态**：

| 值 | 含义 |
|---|---|
| 2 | 待审核 |
| 3 | 待修改 |
| 5 | 商品下架 |
| 6 | 作品过期 |
| 7 | 作品违规 |

**逻辑删除位隔离**：作品含用户上传的图片与设计数据，删除需与分享状态联动。

### wms_work_image 作品图槽位

| 字段 | 类型 | 说明 |
|---|---|---|
| `work_id` | bigint | |
| `slot_index` | int | 槽位序号 |
| `image_url` | varchar(500) | 原图 |
| `thumb_url` | varchar(500) | |
| `copies` | int | **该图的复印份数** |
| `pHash` | bigint | **感知哈希**，用于黑名单比对 |

> **图槽位模型是刻意的设计**：饭圈有两种真实需求——「20 张各不相同的图」（套卡）与「1 张图印 20 份」（送人/换卡）。一个明细下挂多个「图-份数」对，两种都能装；若按任一种建模，另一种来了就要改表。

### wms_collection 作品合集

| 字段 | 类型 | 说明 |
|---|---|---|
| `user_id` / `collection_name` / `cover_url` | | |
| `status` | tinyint | 同作品 |

`wms_collection_item`：`collection_id` / `work_id`。

> 合集规则：**2–10 个作品**成组（「至少选中两个」「最多选中10个」），且**需合并下单**（「合集需合并下单」「合集可单个下单」两种模式）。

### wms_share_code 分享码

| 字段 | 类型 | 说明 |
|---|---|---|
| `share_code` | varchar(32) | 唯一，用户可见的码 |
| `user_id` | bigint | **分享者** |
| `work_id` / `collection_id` | bigint | 分享内容，二选一 |
| `**share_type**` | tinyint | **1 普通 / 2 无料 / 3 合集** |
| `**payer_type**` | tinyint | **1 领取者付商品费+邮费 / 2 分享者付商品费、领取者付邮费** |
| `max_order_qty` | int | **可下单套数上限** |
| `used_qty` | int | 已下单套数 |
| `expire_time` | datetime | 有效期 |
| `status` | tinyint | 见下方失效原因 |
| `invalid_reason` | varchar(100) | |
| `**prepay_amount**` | decimal(12,2) | **分享者预付的制品费总额**（无料时） |
| `**prepay_used**` | decimal(12,2) | **已核销部分** |
| `**prepay_refunded**` | decimal(12,2) | **已退还部分** |

**失效原因**（本系统自拟，写入 `invalid_reason`）：

```
分享码已超过有效期
关联作品已封禁
分享码已达下单上限
```

> **`share_type = 2`（无料分享）是本系统最特殊的业务**：
> - 规则：分享者支付商品费用，领取者只支付邮费
> - 对照组：`领取者支付商品费用与邮费`（普通分享）
> - 即：**作者掏钱请客，领取者只付邮费**，这是饭圈「无料」的本意
> - 分享者**创建分享时即付款**
> - 预付金额被拆成「已核销 / 未核销」，未核销部分**可退**（默认退回，见 §5.5）

### wms_proof_order 打样单

打样单本身复用 `oms_order`，通过 `oms_order.order_type = 2` 标记，此处仅说明其语义：

- **打样 = 用户自己印一份看实物效果**
- **是发放分享码的前置条件**
- 打样券发放给通过认证的团长/摊主

---

## 5. 订单

### 5.1 oms_order 订单主表

| 字段 | 类型 | 说明 |
|---|---|---|
| `order_no` | varchar(32) | 唯一 |
| `user_id` | bigint | 下单人 |
| `**order_type**` | tinyint | **1 普通 / 2 打样 / 3 无料领取 / 4 现货** |
| `**status**` | tinyint | 见第 03 篇状态机 |
| ``payer_user_id`` | bigint | **实际付款人**（无料时 ≠ user_id） |
| `total_amount` | decimal(12,2) | 商品总额 |
| `freight_amount` | decimal(12,2) | 运费 |
| `discount_amount` | decimal(12,2) | 优惠 |
| `pay_amount` | decimal(12,2) | 实付 |
| `refund_amount` | decimal(12,2) | 已退 |
| `coupon_id` | bigint | |
| `group_id` | bigint | 拼团，可空 |
| `share_code_id` | bigint | 来源分享码，可空 |
| `activity_id` | bigint | 秒杀等活动，可空 |
| `channel_id` | bigint | **渠道归因** |
| `pay_time` / `pay_type` / `transaction_id` | | 支付信息 |
| `expire_time` | datetime | **待付款超时点** |
| `**receiver_snapshot**` | json | **收货地址快照** |
| `remark` | varchar(255) | |

**索引**：`uk_order_no`、`idx_user_status`、`idx_status_expire`（兜底扫描用）、`idx_pay_time`（对账用）。

### 5.2 oms_order_item 订单明细

| 字段 | 类型 | 说明 |
|---|---|---|
| `order_id` / `order_no` | | |
| `spu_id` / `sku_id` | bigint | |
| `goods_name` / `sku_spec` / `image` | | **下单快照** |
| `work_id` | bigint | 关联作品 |
| `activity_id` | bigint | 本行活动（秒杀按行抢独立配额，取消 / 退款按行归还，见 03 篇 §1.3） |
| `qty` | int | |
| `unit_price` / `total_price` | decimal(12,2) | **下单时快照** |
| `craft_snapshot` | json | 工艺与加价快照 |
| `pricing_snapshot` | json | 计价方式与参数快照 |

### 5.3 oms_order_image 订单图片

`order_item_id` / `slot_index` / `image_url` / `copies` / `pHash`。

> 从作品图槽位**复制**而来，保证订单与作品后续修改解耦。

### 关键口径裁定

#### ① 地址存快照，不做外键

`oms_order.receiver_snapshot` 存 JSON 快照。**理由**：用户随时可改收货地址，若订单持有外键，改地址会污染历史订单——已发货的订单会显示新地址，对账和客服都出事。

#### ② 库存扣减时机

| 商品类型 | 规则 |
|---|---|
| **定制品** | **不占 SKU 库存**，只占原材料（v1 不做原材料核算） |
| **现货** | 下单即扣，超时未付款自动释放（随取消任务归还） |
| **拼团** | 按**成团人数**判定，不看库存 |

#### ③ 价格全快照

`unit_price` / `craft_snapshot` / `pricing_snapshot` 均在**下单瞬间固化**。SKU 改价不影响历史订单。

#### ④ 对账口径

| 订单类型 | 成交日期取 |
|---|---|
| 普通订单 | **支付时间** `pay_time` |
| 拼团订单 | **成团时间** |

**理由**：普通单按支付时间可与微信支付 T+1 结算流水逐笔核；拼团单在成团前那笔钱是「待定」状态，用支付时间会产生「卖了又退」的虚账。

**退款必须记独立流水**（见 `fin_refund_ledger`），不得直接冲减销售额。

#### ⑤ 无料分享预付款

分享者创建分享即付款，钱进平台，记入 `wms_share_code.prepay_amount`。领取者下单核销时，`prepay_used` 递增。

**退款规则**：`prepay_amount - prepay_used - prepay_refunded` 为可退余额。**默认退未核销部分，已核销不退**。

> 【待确认】是否可退最终取决于 `freeShareConfig` 中的价格档位（分享价是否低于零售价）。设计为**可配置规则**，默认按「可退」实现。

### 5.4 oms_delivery 发货

| 字段 | 类型 | 说明 |
|---|---|---|
| `order_id` | bigint | |
| `delivery_company` / `delivery_no` | | |
| `delivery_time` / `receive_time` | datetime | |
| `status` | tinyint | 1 已发货 / 2 已签收 / 3 退回 |

**索引**：`idx_delivery_no`（查物流用）。

> 顾客端有「超时未取件 / 地址不详等个人原因造成的快递退回需补邮费后重新寄出」的规则，`status = 3` 覆盖此场景。

### 5.5 oms_aftersale 售后

| 字段 | 类型 | 说明 |
|---|---|---|
| `order_id` / `order_item_id` | bigint | 支持**按明细退** |
| `aftersale_no` | varchar(32) | 唯一 |
| `type` | tinyint | 1 退款 / 2 退货退款 / 3 **色差** / 4 其他 |
| `reason` / `description` / `images` | | |
| `amount` | decimal(12,2) | 退款金额 |
| `status` | tinyint | 1 待审 / 2 通过 / 3 驳回 / 4 完成 |
| `audit_user_id` / `audit_time` / `audit_remark` | | |
| `refund_no` | varchar(32) | 微信退款单号 |
| `refund_time` | datetime | |

### 5.6 oms_invoice 发票

| 字段 | 类型 | 说明 |
|---|---|---|
| `invoice_no` | varchar(32) | |
| `user_id` / `order_ids` | | 可合并开票 |
| `title_type` | tinyint | 1 个人 / 2 企业 |
| `title` / `tax_no` | varchar(100) | |
| `amount` | decimal(12,2) | |
| `email` | varchar(100) | |
| `type` | tinyint | 1 电子 / 2 纸质 |
| `status` | tinyint | 1 待审 / 2 已开 / 3 驳回 / 4 已取消 |
| `file_url` | varchar(255) | 电子发票文件 |

> 顾客端有完整开票功能（申请 / 列表 / 记录 / 详情），**必须进 v1**——这是已上线的对外承诺。

### 5.7 oms_order_status_log 状态流转日志

`order_id` / `from_status` / `to_status` / `operator_type`（系统/管理员/用户）/ `operator_id` / `remark` / `create_time`。

> 用于排查与客服答疑：「我的订单做到哪了」必须能回溯。

---

## 6. 审核

### aud_image_audit 图片审核记录

| 字段 | 类型 | 说明 |
|---|---|---|
| `biz_type` | tinyint | 1 定制图 / 2 作品 / 3 头像 |
| `biz_id` | bigint | |
| `image_url` / `pHash` | | |
| `auto_result` | tinyint | 1 通过 / 2 疑似 / 3 违规 |
| `auto_remark` | varchar(255) | 命中原因 |
| `manual_result` | tinyint | 1 通过 / 2 驳回 |
| `audit_user_id` / `audit_time` / `audit_remark` | | |

> 顾客端已有 `file/auditImg` 端点，会返回审核不通过结论及命中的违规关键词；后台侧提示文案自拟（见 05 篇 §2）。

### aud_blacklist 违规图库

| 字段 | 类型 | 说明 |
|---|---|---|
| `pHash` | bigint | 感知哈希，索引 |
| `source` | varchar(50) | 来源 |
| `remark` | varchar(255) | |
| `status` | tinyint | |

> **pHash 比对是廉价且真正有用的拦截**：已知侵权图直接驳回，不进人工队列。

### aud_sensitive_word 敏感词

`word` / `category` / `action`（替换 / 拒绝）/ `status`。

> 顾客端文本审核会提示命中的违规关键词，要求修改后重新提交。

### aud_copyright_promise 版权承诺

| 字段 | 类型 | 说明 |
|---|---|---|
| `user_id` | bigint | |
| `biz_type` / `biz_id` | | |
| `promise_version` | varchar(20) | 承诺书版本 |
| `ip` | varchar(50) | |
| `create_time` | datetime | |

> **每次上传都记一条**。既是合规要求，也是出事时的凭证。

---

## 7. 营销

### 共用活动骨架

拼团、秒杀、优惠券、邀请好友**共用一套活动表**，靠 `activity_type` 区分。

### mkt_activity 活动

| 字段 | 类型 | 说明 |
|---|---|---|
| `activity_name` | varchar(100) | |
| `**activity_type**` | tinyint | **1 拼团 / 2 秒杀 / 3 优惠券 / 4 邀请好友** |
| `spu_ids` / `sku_ids` | json | 参与商品 |
| `start_time` / `end_time` | datetime | |
| `rule_config` | json | **各自类型的规则** |
| `total_stock` | int | 秒杀独立库存总量，`NULL` 表示不限量（只有 `activity_type=2` 用） |
| `sold_stock` | int | 秒杀已占用的配额，下单 +、取消/审核驳回 −，**只由服务端条件更新维护，客户端不可写** |
| `status` | tinyint | 0 停用 / 1 进行 / 2 结束 |

> 秒杀配额为什么单独建两列而不写在 `rule_config` 里：`rule_config` 是可编辑的配置，扣减要落在一个可写 `WHERE sold_stock + n <= total_stock` 的单行条件更新上，才能防超卖；JSON 字段做不到。缩减 `total_stock` 到低于 `sold_stock` 会被拒绝。

**`rule_config` 内容**：

| 类型 | 字段 |
|---|---|
| 拼团 | `groupSize`（默认 3）、`durationHours`（默认 24）、`simulateEnabled`（默认 false）、`groupPrice` |
| 秒杀 | `seckillPrice`、`perUserLimit`、`sessions`（场次）；库存配额走 `total_stock` 列，不放 `rule_config` |
| 优惠券 | `couponType`、`threshold`、`amount` / `discount`、`validDays`、`totalQty` |
| 邀请好友 | `inviteeReward`、`inviterReward`、`maxInvite` |

> 拼团规则：**满 3 人付款才开始制作，不足 3 人则拼团失败、24 小时后自动退款**。默认值照此。

### mkt_group 拼团

| 字段 | 类型 | 说明 |
|---|---|---|
| `activity_id` | bigint | |
| `leader_user_id` | bigint | 团长 |
| `target_size` / `current_size` | int | |
| `**status**` | tinyint | **1 拼团中 / 2 已成团 / 3 拼团失败** |
| `expire_time` | datetime | |
| `is_simulated` | tinyint | **是否模拟成团** |

### mkt_group_member 团员

`group_id` / `user_id` / `order_id` / `is_leader` / `is_virtual`（模拟成团时置 1）/ `join_time` / `status`。

> **模拟成团**（拼多多式）：到期时若人数不足，注入虚拟团员使其成团，避免退款成本与用户失望。**做成开关，默认关闭**——涉及「假装有用户」，需业务方确认合规性。

### mkt_coupon / mkt_coupon_user

`mkt_coupon`：`activity_id` / `coupon_name` / `coupon_type`（1 满减 / 2 折扣 / 3 无门槛）/ `threshold` / `amount` / `discount` / `valid_days` / `total_qty` / `issued_qty`。

`mkt_coupon_user`：`coupon_id` / `user_id` / `code` / `status`（1 未使用 / 2 已使用 / 3 已过期）/ `use_time` / `order_id` / `expire_time` / `source`（1 人工发放 / 2 自动发放 / 3 领券中心）。

**互斥规则**：**拼团价与优惠券互斥**。拼团已让利，再叠券会击穿成本，且规则一复杂用户算不清、客服解释不清。

> 顾客端有 `couponCenter`（领券中心）页面，属顾客端功能，v1 后台只做发放与核销管理。

---

## 8. 客服

### svc_ticket 工单

| 字段 | 类型 | 说明 |
|---|---|---|
| `ticket_no` | varchar(32) | 唯一 |
| `user_id` | bigint | |
| `type` | tinyint | 1 售后咨询 / 2 **侵权投诉** / 3 色差投诉 / 4 其他 |
| `order_id` | bigint | **可空** |
| `title` / `content` / `images` | | |
| `**status**` | tinyint | **1 待受理 / 2 处理中 / 3 已解决 / 4 已关闭** |
| `handler_id` | bigint | 处理人 |
| `source` | tinyint | 1 用户提交 / 2 客服代建 |

**工单必须可选关联订单**：咨询类问题常常还没下单，强制挂单会把这类堵死。

### svc_ticket_reply 工单回复

`ticket_id` / `reply_type`（1 客服 / 2 用户）/ `user_id` / `content` / `images` / `create_time`。

> 侵权投诉的处置结论（下架 / 驳回）记在回复与工单状态里。**不做 DMCA 式复杂工作流**。

---

## 9. 财务

### fin_refund_ledger 退款流水

| 字段 | 类型 | 说明 |
|---|---|---|
| `refund_no` | varchar(32) | 唯一 |
| `biz_type` | tinyint | 1 订单退款 / 2 拼团失败退款 / 3 **无料预付款退回** / 4 售后 |
| `biz_id` | bigint | |
| `order_id` / `user_id` | | |
| `amount` | decimal(12,2) | |
| `channel` | tinyint | 1 微信原路退回 |
| `status` | tinyint | 1 处理中 / 2 成功 / 3 失败 |
| `remark` | varchar(255) | |
| `review_status` | tinyint | **0 无需复核 / 1 待复核 / 2 复核通过 / 3 复核驳回** |
| `reviewer_id` | bigint | 复核人 `sys_user.id` |
| `review_time` | datetime | |
| `review_remark` | varchar(255) | 复核意见 |

> **强制退款是两段式的**：运营点「强制退款」只写一条 `review_status=1` 的流水，钱一分不动；财务在「退款流水」页复核，通过后条件更新 `review_status 1→2` 抢到行的人才继续出款，订单进 11 已退款，驳回（`1→3`）则订单状态不变。这样两个财务同时点通过也只有一人成功，且申请人无法自审自批（`order:refund:review` 是独立权限）。

### fin_daily_settlement 日结

`settle_date` / `order_amount`（支付口径）/ `groupon_amount`（成团口径）/ `refund_amount` / `net_amount` / `order_count`。

> 与微信支付 T+1 结算流水逐笔核对。

---

## 10. 延迟任务

### tsk_delay_log 延迟任务日志

| 字段 | 类型 | 说明 |
|---|---|---|
| `task_type` | varchar(50) | 如 `ORDER_UNPAID_CANCEL` |
| `biz_id` | bigint | 业务主键 |
| `fire_time` | datetime | 预定触发时间 |
| `exec_time` | datetime | 实际执行时间 |
| `status` | tinyint | 1 待执行 / 2 成功 / 3 失败 |
| `retry_count` | int | |
| `error_msg` | text | |

**唯一键**：`uk_type_biz (task_type, biz_id)`——**用于幂等与去重**。

**在用的 `task_type`**（常量定义在各自的 handler / service 上，不在这里另抄一份枚举）：

| task_type | 触发什么 | 登记时机 |
|---|---|---|
| `ORDER_UNPAID_CANCEL` | 待付款超时自动取消 | 下单时 `schedule()` |
| `ORDER_AUTO_CONFIRM` | 发货后自动确认收货 | **仅兜底扫描**（发货处未 `schedule()`） |
| `ORDER_DELIVERY_ALERT` | 待发货超时告警（超过 `pendingDeliveryHours`） | 订单进「待发货」时 `schedule()` |
| `GROUP_EXPIRE` | 拼团到期：满员成团 / 不足则失败退款 | **仅兜底扫描**（参团入口还没有建团的 `schedule()` 调用） |
| `COUPON_EXPIRE` | 优惠券到期置失效 | 领券后 `schedule()` |
| `SHARE_CODE_EXPIRE` | 分享码到期失效并退未核销预付款 | **仅兜底扫描**（后台无建分享码端点） |
| `WORK_EXPIRE` | 作品到期下架 | **仅兜底扫描**（后台无建作品端点） |

> 每个 handler 还必须实现 `scanOverdue()`：进程重启或延时队列漏掉时，5 分钟一次的兜底扫描按「已过期但状态还没改」直接捞出来处理。标「仅兜底扫描」的三类，等补齐创建端点（P3）后要在创建处补 `schedule()`，否则它们只能靠这一条路径生效。

> 这张表有三个作用：① 第 03 层兜底扫描的查询依据；② 任务执行失败后可重试（弥补 DelayQueue 静默丢任务的缺口）；③ 将来迁移到 MQ 时的**消费端去重依据**。

---

## 11. 关键索引清单

| 表 | 索引 | 用途 |
|---|---|---|
| `oms_order` | `uk_order_no` | 幂等 |
| `oms_order` | `idx_status_expire (status, expire_time)` | **兜底扫描** |
| `oms_order` | `idx_pay_time` | **对账** |
| `oms_order` | `idx_user_status` | 用户查单 |
| `wms_share_code` | `uk_share_code` | 码查询 |
| `wms_share_code` | `idx_status_expire` | 到期失效 |
| `mkt_group` | `idx_status_expire` | 拼团到期 |
| `tsk_delay_log` | `uk_type_biz` | 幂等 |
| `aud_blacklist` | `idx_pHash` | 黑名单比对 |
| `oms_delivery` | `idx_delivery_no` | 物流查询 |
