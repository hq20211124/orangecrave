package com.orangecrave.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 静态管理端页面冒烟：确认 admin-web 资源被打包并可由 Spring Boot 以 /admin/** 托管，
 * 且新增页面确实存在（返回 200 且包含 page-title 骨架）。  */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminWebPagesTest {

    @Autowired
    private TestRestTemplate rest;

    private static final String[] PAGES = {
            "index.html",
            "layout.html",
            "pages/order/list.html",
            "pages/order/production.html",
            "pages/order/delivery.html",
            "pages/order/aftersale.html",
            "pages/order/invoice.html",
            "pages/work/list.html",
            "pages/work/share-code.html",
            "pages/work/proof.html",
            "pages/audit/record.html",
            "pages/audit/blacklist.html",
            "pages/marketing/activity.html",
            "pages/marketing/activity-edit.html",
            "pages/marketing/seckill.html",
            "pages/marketing/coupon.html",
            "pages/marketing/invite.html",
            "pages/service/ticket.html",
            "pages/service/ticket-detail.html",
            "pages/finance/dashboard.html",
            "pages/finance/settlement.html",
            "pages/finance/refund.html",
            "pages/user/list.html",
            "pages/user/detail.html",
            "pages/user/cert.html",
            "pages/system/user.html",
            "pages/system/role.html",
            "pages/system/menu.html",
            "pages/system/task-log.html",
            "pages/goods/series.html",
            "pages/goods/category.html",
            "pages/goods/spu-edit.html"
    };

    @Test
    void allAdminPagesServed() {
        for (String page : PAGES) {
            ResponseEntity<String> res = rest.getForEntity("/admin/" + page, String.class);
            assertThat(res.getStatusCode().value())
                    .as("/admin/%s 应可访问", page).isEqualTo(200);
            // 业务页面应包含统一骨架；登录页/框架页除外
            if (page.startsWith("pages/")) {
                assertThat(res.getBody()).as("/admin/%s 应包含页面骨架", page).contains("page-title");
            }
        }
    }
}
