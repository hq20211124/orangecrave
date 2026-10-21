# 07 — 真实渠道集成

> 目标：支付、微信登录、物流、MES 从桩（Stub）切换到真实渠道。

## 1. 现状

系统中的外部依赖全部是桩实现：

| 依赖 | 桩实现 | 真实渠道 |
|------|--------|----------|
| 支付 | `StubPaymentAdapter` | 微信支付 / 支付宝 |
| 微信登录 | 1.0 阶段桩，未接 SDK | 微信开放平台的 code2session |
| 物流 | `StubDeliveryAdapter` | 顺丰 / 中通 / 圆通 API |
| MES | `ManualProductionAdapter` | 生产执行系统 API |
| 短信 | 无 | 阿里云短信 / 腾讯云短信 |
| 对象存储 | `LocalFileStorage` | 阿里云 OSS / AWS S3 |

## 2. 支付集成（微信支付）

### 2.1 前置条件

1. 注册微信支付商户号：https://pay.weixin.qq.com
2. 获取：商户号（mchId）、APIv3 密钥、商户证书（apiclient_cert.p12）
3. 小程序 AppID 和 AppSecret

### 2.2 添加依赖

在 `orangecrave-order/pom.xml`：

```xml
<dependency>
    <groupId>com.github.wechatpay-apiv3</groupId>
    <artifactId>wechatpay-apache-httpclient</artifactId>
    <version>0.4.9</version>
</dependency>
```

### 2.3 配置

```yaml
orangecrave:
  payment:
    type: wxpay           # stub | wxpay
    wxpay:
      mch-id: ${WXPAY_MCH_ID}
      app-id: ${WX_APP_ID}
      api-v3-key: ${WXPAY_API_V3_KEY}
      cert-serial-no: ${WXPAY_CERT_SERIAL_NO}
      private-key-path: /etc/orangecrave/wxpay/apiclient_key.pem
      notify-url: https://api.orangecrave.example.com/client-api/oms/payment/notify
```

### 2.4 实现 PaymentAdapter 接口

当前 `PaymentAdapter` 是接口，`StubPaymentAdapter` 是桩。新增 `WxPaymentAdapter`：

```java
// 文件：order/src/main/java/com/orangecrave/admin/order/adapter/WxPaymentAdapter.java
package com.orangecrave.admin.order.adapter;

import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.*;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 微信支付适配器。type=wxpay 时生效。
 */
@Component
@ConditionalOnProperty(name = "orangecrave.payment.type", havingValue = "wxpay")
public class WxPaymentAdapter implements PaymentAdapter {

    private final CloseableHttpClient httpClient;
    private final WxPayProperties properties;

    public WxPaymentAdapter(WxPayProperties properties) throws Exception {
        this.properties = properties;
        // 初始化微信支付客户端（自动管理证书）
        PrivateKey merchantPrivateKey = PemUtil.loadPrivateKey(
                new FileInputStream(properties.getPrivateKeyPath()));
        CertificatesManager certificatesManager = CertificatesManager.getInstance();
        certificatesManager.putMerchant(properties.getMchId(),
                new WechatPay2Credentials(properties.getMchId(),
                        new PrivateKeySigner(properties.getCertSerialNo(), merchantPrivateKey)),
                merchantPrivateKey.getEncoded());
        this.httpClient = WechatPayHttpClientBuilder.create()
                .withMerchant(properties.getMchId(), properties.getCertSerialNo(), merchantPrivateKey)
                .withValidator(new WechatPay2Validator(certificatesManager.getVerifier(properties.getMchId())))
                .build();
    }

    @Override
    public PaymentResult pay(String orderSn, BigDecimal amount, String openId) {
        try {
            HttpPost httpPost = new HttpPost("https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi");
            httpPost.addHeader("Content-Type", "application/json");

            String body = String.format("""
                {
                  "appid": "%s",
                  "mchid": "%s",
                  "description": "橙刻定制-%s",
                  "out_trade_no": "%s",
                  "notify_url": "%s",
                  "amount": {"total": %d, "currency": "CNY"},
                  "payer": {"openid": "%s"}
                }
                """, properties.getAppId(), properties.getMchId(),
                orderSn, orderSn, properties.getNotifyUrl(),
                amount.multiply(BigDecimal.valueOf(100)).intValue(),  // 转为分
                openId);

            httpPost.setEntity(new StringEntity(body, "UTF-8"));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            String responseBody = EntityUtils.toString(response.getEntity());

            // 解析 prepay_id，生成小程序调起支付参数
            // ...
            return PaymentResult.success(orderSn, parsePrepayId(responseBody));
        } catch (Exception e) {
            return PaymentResult.fail(orderSn, e.getMessage());
        }
    }
}
```

### 2.5 支付回调

新增回调接口处理支付结果：

```java
// 文件：order/src/main/java/com/orangecrave/admin/order/controller/client/ClientPaymentController.java
@PostMapping("/payment/notify")
public String paymentNotify(@RequestBody String body,
                            @RequestHeader("Wechatpay-Signature") String signature,
                            @RequestHeader("Wechatpay-Timestamp") String timestamp,
                            @RequestHeader("Wechatpay-Nonce") String nonce) {
    // 1. 验证签名（防止伪造回调）
    // 2. 解密回调数据（AES-GCM，用 APIv3 密钥）
    // 3. 更新订单状态为已支付
    // 4. 触发后续流程（通知生产、更新库存）
    // 5. 返回 {"code":"SUCCESS"} 给微信
}
```

### 2.6 对账

每日定时任务下载微信对账单，与本地订单对账：

```java
@Scheduled(cron = "0 0 6 * * ?")   // 每天凌晨 6 点
public void reconcile() {
    // 1. 调用微信支付下载对账单接口
    // 2. 解析 CSV，逐条对比本地订单
    // 3. 标记差异订单（本地有微信没有 / 微信有本地没有 / 金额不一致）
    // 4. 差异超过阈值告警
}
```

## 3. 微信登录集成

### 3.1 流程

```
小程序端                          后端
   │                               │
   │  wx.login() → code            │
   │──────────────────────────────→│
   │                               │  code2session(code)
   │                               │  → openid + session_key
   │                               │
   │  后端生成 token                │
   │←──────────────────────────────│
   │                               │
   │  wx.getUserProfile()          │
   │  → encryptedData + iv         │
   │──────────────────────────────→│
   │                               │  用 session_key 解密
   │                               │  → 用户昵称/头像
```

### 3.2 实现

当前 `/client-api/auth/wx-login` 是桩。替换为真实实现：

```java
@PostMapping("/wx-login")
public Result<LoginVO> wxLogin(@RequestBody WxLoginDTO dto) {
    // 1. 调用微信 code2session 接口
    String url = String.format(
        "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
        appId, appSecret, dto.getCode());
    String response = restTemplate.getForObject(url, String.class);
    JSONObject json = JSON.parseObject(response);

    if (json.containsKey("errcode")) {
        throw new BusinessException("微信登录失败: " + json.getString("errmsg"));
    }

    String openId = json.getString("openid");
    String sessionKey = json.getString("session_key");

    // 2. 查找或创建用户
    UmsUser user = userService.findOrCreateByOpenId(openId);

    // 3. 生成 Sa-Token
    StpClientUtil.login(user.getId());
    String token = StpClientUtil.getTokenValue();

    return Result.success(new LoginVO(token, user.getId(), user.getNickname()));
}
```

## 4. 物流集成

### 4.1 选择物流服务商

| 服务商 | 接入方式 | 特点 |
|--------|----------|------|
| 快递鸟 | 统一 API，聚合多家快递 | 一个接口查所有快递 |
| 顺丰 | 官方 API | 直营，时效好 |
| 中通 | 官方 API | 价格低 |
| 阿里云物流 | 云市场 API | 按调用量计费 |

**推荐快递鸟**（统一接入，避免每家快递单独对接）。

### 4.2 实现 DeliveryAdapter

```java
@Component
@ConditionalOnProperty(name = "orangecrave.delivery.type", havingValue = "kdniao")
public class KdniaoDeliveryAdapter implements DeliveryAdapter {

    @Override
    public DeliveryResult createOrder(String orderSn, Address address, List<OrderItem> items) {
        // 1. 选择快递公司（根据地址/价格策略）
        // 2. 调用快递鸟下单接口
        // 3. 获取运单号
        // 4. 返回运单号 + 快递公司编码
    }

    @Override
    public DeliveryTrackResult track(String trackingNo, String shipperCode) {
        // 调用快递鸟轨迹查询接口
    }
}
```

### 4.3 物流回调

快递鸟推送物流轨迹变化到回调地址：

```java
@PostMapping("/delivery/notify")
public Result<Void> deliveryNotify(@RequestBody KdniaoNotifyDTO dto) {
    // 1. 验证签名
    // 2. 更新订单物流状态
    // 3. 如果已签收，触发自动确认收货
}
```

## 5. MES 集成

### 5.1 现状

当前是 `ManualProductionAdapter`，生产状态靠人工在后台手动更新。

### 5.2 真实 MES 对接

如果公司有 MES 系统（如 SAP ME、Siemens Opcenter、自研 MES），对接方式：

```
OrangeCrave                    MES
    │                           │
    │  1. 创建生产工单            │
    │──────────────────────────→│
    │                           │
    │  2. MES 确认接单           │
    │←──────────────────────────│
    │                           │
    │  3. MES 推送生产进度        │
    │  （开始生产/质检/完成）      │
    │←──────────────────────────│
    │                           │
    │  4. MES 通知发货           │
    │←──────────────────────────│
```

实现方式：
- **拉模式**：定时轮询 MES 接口获取工单状态
- **推模式**：MES 回调 OrangeCrave 的 Webhook

```java
@PostMapping("/mes/notify")
public Result<Void> mesNotify(@RequestBody MesNotifyDTO dto) {
    // 验证签名 → 更新生产状态 → 触发后续流程
}
```

## 6. 集成配置汇总

所有真实渠道的开关集中在 `application-prod.yml`：

```yaml
orangecrave:
  payment:
    type: wxpay
  delivery:
    type: kdniao
  storage:
    type: s3
  redis:
    enabled: true
  mq:
    type: redis-stream
```

**每个适配器用 `@ConditionalOnProperty` 控制，默认值全部是 `stub`/`local`/`noop`。不配置就不生效，不会影响测试和开发环境。**

## 7. 上线前集成测试

| 集成 | 测试项 | 验证方法 |
|------|--------|----------|
| 微信支付 | 下单→支付→回调→状态更新 | 用微信支付沙箱环境测试 |
| 微信登录 | code→openid→token | 用微信开发者工具测试 |
| 物流 | 下单→获取运单号→轨迹查询 | 快递鸟测试账号 |
| MES | 工单创建→状态同步→完成通知 | MES 测试环境 |
| 对账 | 支付对账 / 物流对账 | 手动触发对账任务，检查差异 |

## 8. 检查清单

- [ ] 微信支付商户号已注册，APIv3 密钥已配置
- [ ] 支付回调地址可达，签名验证通过
- [ ] 微信登录 code2session 可用
- [ ] 物流下单和轨迹查询可用
- [ ] MES 对接已联调
- [ ] 对账任务已配置，差异告警已接入
- [ ] 所有桩实现可通过配置切换回真实实现
- [ ] 敏感配置（密钥/证书）通过 KMS 管理
