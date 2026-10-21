package com.orangecrave.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.order.constant.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理端缺失接口补齐后的集成测试（H2 + 种子）：
 * 先用顾客端主链路造一单，再验证改价/备注/生产工单/发货面单/强制退款，
 * 以及秒杀、邀请、地址、导出等新增只读/写接口的可用性。  */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminOrderOpsTest {

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper json = new ObjectMapper();

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        // 写操作必须带幂等键（02 篇 §1.5），每个请求一个独立键
        h.set("Idempotency-Key", java.util.UUID.randomUUID().toString());
        if (token != null) {
            h.set("Authorization", "Bearer " + token);
        }
        return h;
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        return json.readTree(response.getBody());
    }

    private String adminToken() throws Exception {
        ResponseEntity<String> login = rest.postForEntity("/admin-api/auth/login",
                new HttpEntity<>("{\"username\":\"admin\",\"password\":\"admin123\"}", headers(null)), String.class);
        JsonNode data = body(login).path("data");
        assertThat(data.path("token").asText()).isNotBlank();
        return data.path("token").asText();
    }

    /** 顾客端下单并支付，返回订单 id。 */
    private long createPaidOrder() throws Exception {
        ResponseEntity<String> login = rest.postForEntity("/client-api/auth/wx-login",
                new HttpEntity<>("{\"code\":\"ops-user\",\"channelId\":1}", headers(null)), String.class);
        String clientToken = body(login).path("data").path("token").asText();

        String submitBody = "{\"skuId\":1,\"qty\":10,\"channelId\":1,"
                + "\"receiverSnapshot\":\"{\\\"name\\\":\\\"演示\\\"}\","
                + "\"promiseVersion\":\"v1.0\","
                + "\"images\":[{\"slotIndex\":0,\"imageUrl\":\"http://x/1.png\",\"copies\":10}]}";
        ResponseEntity<String> submit = rest.postForEntity("/client-api/oms/order/submit",
                new HttpEntity<>(submitBody, headers(clientToken)), String.class);
        long orderId = body(submit).path("data").path("id").asLong();
        assertThat(orderId).isPositive();

        ResponseEntity<String> pay = rest.postForEntity("/client-api/oms/order/" + orderId + "/pay",
                new HttpEntity<>(headers(clientToken)), String.class);
        assertThat(body(pay).path("code").asInt()).isZero();
        return orderId;
    }

    @Test
    void adminOpsEndpoints() throws Exception {
        String token = adminToken();
        long orderId = createPaidOrder();

        // 备注
        ResponseEntity<String> remark = rest.exchange("/admin-api/oms/order/" + orderId + "/remark",
                HttpMethod.PUT, new HttpEntity<>("{\"remark\":\"加急处理\"}", headers(token)), String.class);
        assertThat(body(remark).path("code").asInt()).isZero();

        // 改价（记快照）
        ResponseEntity<String> price = rest.exchange("/admin-api/oms/order/" + orderId + "/price",
                HttpMethod.PUT, new HttpEntity<>("{\"payAmount\":99.00,\"reason\":\"协商改价\"}", headers(token)), String.class);
        assertThat(body(price).path("code").asInt()).isZero();
        ResponseEntity<String> detail = rest.exchange("/admin-api/oms/order/" + orderId, HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(detail).path("data").path("payAmount").decimalValue()).isEqualByComparingTo("99.00");
        assertThat(body(detail).path("data").path("remark").asText()).isEqualTo("加急处理");

        // 录入 MES 工单号
        ResponseEntity<String> workOrderNo = rest.postForEntity("/admin-api/oms/production/" + orderId + "/work-order-no",
                new HttpEntity<>("{\"workOrderNo\":\"MES-0001\"}", headers(token)), String.class);
        assertThat(body(workOrderNo).path("code").asInt()).isZero();
        ResponseEntity<String> detail2 = rest.exchange("/admin-api/oms/order/" + orderId, HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(detail2).path("data").path("workOrderNo").asText()).isEqualTo("MES-0001");

        // 手工触发 MES 同步（不带车间术语时只刷同步时间）
        ResponseEntity<String> sync = rest.postForEntity("/admin-api/oms/production/sync?orderId=" + orderId,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(sync).path("code").asInt()).isZero();
        // 待审核的单还没进车间，MES 术语同步必须被状态机拦住（03 篇 §4.3）
        ResponseEntity<String> badSync = rest.postForEntity("/admin-api/oms/production/sync?orderId=" + orderId,
                new HttpEntity<>("{\"mesStatus\":\"已开料\"}", headers(token)), String.class);
        assertThat(body(badSync).path("code").asInt()).isNotZero();

        // 审核放行后进车间，车间术语落到后台状态、对用户只给粗粒度文案
        ResponseEntity<String> auditPass = rest.exchange("/admin-api/oms/production/" + orderId + "/audit-pass",
                HttpMethod.PUT, new HttpEntity<>(headers(token)), String.class);
        assertThat(body(auditPass).path("code").asInt()).isZero();
        ResponseEntity<String> mesSync = rest.postForEntity("/admin-api/oms/production/sync?orderId=" + orderId,
                new HttpEntity<>("{\"mesStatus\":\"裱纸中\"}", headers(token)), String.class);
        assertThat(body(mesSync).path("data").asText()).isEqualTo("正在制作");
        ResponseEntity<String> producing = rest.exchange("/admin-api/oms/order/" + orderId, HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(producing).path("data").path("status").asInt())
                .isEqualTo(OrderStatus.PRODUCING);
        // MES 术语表（前端下拉用，03 篇 §4.3）
        ResponseEntity<String> mesMap = rest.exchange("/admin-api/oms/production/mes-status", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(mesMap).path("data").has("已开料")).isTrue();

        // 打样单分页（本单为普通单，应返回空列表但不报错）
        ResponseEntity<String> proof = rest.exchange("/admin-api/wms/proof/page", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(proof).path("code").asInt()).isZero();

        // 用户订单（新接口，位于 order 模块承载的 ums 路径）
        ResponseEntity<String> userOrders = rest.exchange("/admin-api/ums/user/1/orders", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(userOrders).path("code").asInt()).isZero();

        // 电子面单打印 + 物流轨迹（桩）
        ResponseEntity<String> print = rest.exchange("/admin-api/oms/delivery/print?orderIds=" + orderId,
                HttpMethod.GET, new HttpEntity<>(headers(token)), String.class);
        assertThat(body(print).path("data").size()).isEqualTo(1);
        String deliveryNo = body(print).path("data").get(0).path("deliveryNo").asText();
        assertThat(deliveryNo).isNotBlank();
        ResponseEntity<String> track = rest.exchange("/admin-api/oms/delivery/track?no=" + deliveryNo,
                HttpMethod.GET, new HttpEntity<>(headers(token)), String.class);
        assertThat(body(track).path("data").size()).isGreaterThanOrEqualTo(1);

        // 批量导出（发货用）
        ResponseEntity<String> export = rest.postForEntity("/admin-api/oms/order/batch-export",
                new HttpEntity<>("{\"ids\":[" + orderId + "]}", headers(token)), String.class);
        assertThat(body(export).path("data").size()).isEqualTo(1);

        // 秒杀：新增 + 分页
        ResponseEntity<String> seckill = rest.postForEntity("/admin-api/mkt/seckill",
                new HttpEntity<>("{\"activityName\":\"双11秒杀\",\"ruleConfig\":\"{}\"}", headers(token)), String.class);
        assertThat(body(seckill).path("code").asInt()).isZero();
        ResponseEntity<String> seckillPage = rest.exchange("/admin-api/mkt/seckill/page", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(seckillPage).path("data").path("total").asLong()).isGreaterThanOrEqualTo(1L);

        // 邀请记录 + 拉新统计
        ResponseEntity<String> invitePage = rest.exchange("/admin-api/mkt/invite/page", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(invitePage).path("code").asInt()).isZero();
        ResponseEntity<String> inviteStat = rest.exchange("/admin-api/mkt/invite/statistics", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(inviteStat).path("data").path("totalInvites").asLong()).isGreaterThanOrEqualTo(0L);

        // 收货地址只读分页
        ResponseEntity<String> address = rest.exchange("/admin-api/ums/address/page", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(address).path("code").asInt()).isZero();

        // 财务退款流水导出 + 操作日志导出
        ResponseEntity<String> refundExport = rest.exchange("/admin-api/fin/refund/export", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(refundExport).path("code").asInt()).isZero();
        ResponseEntity<String> operExport = rest.exchange("/admin-api/system/oper-log/export", HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(operExport).path("code").asInt()).isZero();

        // 强制退款两段式（03 篇 §3.3）：申请只落一条待复核流水，钱要等财务复核
        ResponseEntity<String> apply = rest.postForEntity("/admin-api/oms/order/" + orderId + "/force-refund",
                new HttpEntity<>("{\"amount\":10.00,\"reason\":\"客诉全额退\"}", headers(token)), String.class);
        assertThat(body(apply).path("code").asInt()).isZero();
        long refundId = body(apply).path("data").asLong();
        assertThat(refundId).isPositive();
        ResponseEntity<String> afterApply = rest.exchange("/admin-api/oms/order/" + orderId, HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(afterApply).path("data").path("status").asInt()).isNotEqualTo(11);

        // 同一张单不能再挂第二笔待复核申请
        ResponseEntity<String> duplicate = rest.postForEntity("/admin-api/oms/order/" + orderId + "/force-refund",
                new HttpEntity<>("{\"amount\":10.00,\"reason\":\"重复申请\"}", headers(token)), String.class);
        assertThat(body(duplicate).path("code").asInt()).isNotZero();

        // 复核通过才真退：订单转已退款、退款金额累加
        ResponseEntity<String> review = rest.postForEntity("/admin-api/oms/order/force-refund/" + refundId + "/review",
                new HttpEntity<>("{\"approved\":true,\"remark\":\"同意\"}", headers(token)), String.class);
        assertThat(body(review).path("code").asInt()).isZero();
        assertThat(body(review).path("data").asText()).startsWith("RF");
        ResponseEntity<String> afterRefund = rest.exchange("/admin-api/oms/order/" + orderId, HttpMethod.GET,
                new HttpEntity<>(headers(token)), String.class);
        assertThat(body(afterRefund).path("data").path("status").asInt()).isEqualTo(11);
        assertThat(body(afterRefund).path("data").path("refundAmount").decimalValue()).isEqualByComparingTo("10.00");

        // 复核只能做一次
        ResponseEntity<String> again = rest.postForEntity("/admin-api/oms/order/force-refund/" + refundId + "/review",
                new HttpEntity<>("{\"approved\":true,\"remark\":\"重复复核\"}", headers(token)), String.class);
        assertThat(body(again).path("code").asInt()).isNotZero();
    }
}
