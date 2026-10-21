package com.orangecrave.admin;

import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.user.entity.UmsIdentityCert;
import com.orangecrave.admin.user.mapper.UmsIdentityCertMapper;
import com.orangecrave.admin.user.model.vo.UmsIdentityCertVO;
import com.orangecrave.admin.user.service.UmsCertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 身份证号全链路（01 篇 §2 加密存储 / 02 篇 §2.3 出参脱敏）：
 * 落库是 AES-GCM 密文，实体读回是明文，接口出参只剩掩码。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SensitiveDataFlowTest {

    @Autowired
    private UmsIdentityCertMapper certMapper;

    @Autowired
    private UmsCertService certService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void idCardIsEncryptedInDatabaseAndMaskedInTheResponse() {
        UmsIdentityCert cert = new UmsIdentityCert();
        cert.setUserId(9001L);
        cert.setIdentityType(1);
        cert.setRealName("测试用户A");
        cert.setIdCardNo("110101199003071234");
        cert.setStatus(1);
        certMapper.insert(cert);

        String column = jdbcTemplate.queryForObject(
                "SELECT id_card_no FROM ums_identity_cert WHERE id = ?", String.class, cert.getId());
        assertThat(column).isNotEqualTo("110101199003071234").doesNotContain("19900307");

        // TypeHandler 对上层透明，业务代码拿到的仍是明文
        assertThat(certMapper.selectById(cert.getId()).getIdCardNo()).isEqualTo("110101199003071234");

        UmsIdentityCertVO vo = certService.get(cert.getId());
        assertThat(vo.getIdCardNo()).isEqualTo("110***********1234");
        assertThat(certService.page(new PageQuery(), null).getList())
                .extracting(UmsIdentityCertVO::getIdCardNo).contains("110***********1234");
    }
}
