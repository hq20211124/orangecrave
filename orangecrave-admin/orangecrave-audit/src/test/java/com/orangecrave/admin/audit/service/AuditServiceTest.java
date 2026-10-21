package com.orangecrave.admin.audit.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.orangecrave.admin.audit.entity.AudBlacklist;
import com.orangecrave.admin.audit.entity.AudImageAudit;
import com.orangecrave.admin.audit.mapper.AudBlacklistMapper;
import com.orangecrave.admin.audit.mapper.AudCopyrightPromiseMapper;
import com.orangecrave.admin.audit.mapper.AudImageAuditMapper;
import com.orangecrave.admin.common.storage.FileStorage;
import com.orangecrave.admin.system.entity.SysConfig;
import com.orangecrave.admin.system.service.SysConfigService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 审核服务：批量审核的批量化、黑名单缓存的一致性。 */
class AuditServiceTest {

    private AudImageAuditMapper imageAuditMapper;
    private AudBlacklistMapper blacklistMapper;
    private FileStorage fileStorage;
    private SysConfigService configService;
    private AuditService service;

    /** 纯单测没有 MyBatis 上下文，LambdaUpdateWrapper 需要先把实体的 lambda 缓存建好。 */
    @BeforeAll
    static void initLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace("com.orangecrave.admin.audit.mapper.AudImageAuditMapper");
        TableInfoHelper.initTableInfo(assistant, AudImageAudit.class);
    }

    @BeforeEach
    void setUp() {
        imageAuditMapper = mock(AudImageAuditMapper.class);
        blacklistMapper = mock(AudBlacklistMapper.class);
        fileStorage = mock(FileStorage.class);
        configService = mock(SysConfigService.class);
        service = new AuditService(imageAuditMapper, blacklistMapper,
                mock(AudCopyrightPromiseMapper.class), fileStorage, configService);
    }

    private AudImageAudit audit(long id, Long pHash) {
        AudImageAudit audit = new AudImageAudit();
        audit.setId(id);
        audit.setPHash(pHash);
        return audit;
    }

    /** 批量通过：整批一次 UPDATE，不再每条各一次。 */
    @Test
    void batchApproveUpdatesAllInOneStatement() {
        when(imageAuditMapper.selectBatchIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(audit(1L, 10L), audit(2L, 20L), audit(3L, 30L)));

        int count = service.batchAudit(List.of(1L, 2L, 3L), 1, "ok", 9L);

        assertThat(count).isEqualTo(3);
        verify(imageAuditMapper, times(1)).update(any(), any());
        verify(imageAuditMapper, never()).updateById(any());
        verify(blacklistMapper, never()).insert(any());
    }

    /** 批量驳回：按 pHash 去重入黑名单，相同哈希不重复插。 */
    @Test
    void batchRejectAddsBlacklistOncePerDistinctHash() {
        when(imageAuditMapper.selectBatchIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(audit(1L, 10L), audit(2L, 10L), audit(3L, 30L)));

        int count = service.batchAudit(List.of(1L, 2L, 3L), 2, "违规", 9L);

        assertThat(count).isEqualTo(3);
        ArgumentCaptor<AudBlacklist> captor = ArgumentCaptor.forClass(AudBlacklist.class);
        verify(blacklistMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(AudBlacklist::getPHash)
                .containsExactly(10L, 30L);
    }

    /** 黑名单读多写少：连续多次比对只查一次库。 */
    @Test
    void blacklistIsLoadedOnceAcrossChecks() throws Exception {
        AudBlacklist black = new AudBlacklist();
        black.setId(5L);
        black.setPHash(123456L);
        when(blacklistMapper.selectList(any())).thenReturn(List.of(black));
        when(fileStorage.load(any())).thenAnswer(inv -> imageBytes());
        when(configService.getByKey("audit.pHashThreshold")).thenReturn(config("5"));

        service.checkBlacklist("a.png");
        service.checkBlacklist("b.png");

        verify(blacklistMapper, times(1)).selectList(any());
    }

    /** 删除黑名单后缓存必须失效，否则已删除的哈希还会拦住上传。 */
    @Test
    void removingBlacklistInvalidatesCache() throws Exception {
        AudBlacklist black = new AudBlacklist();
        black.setId(5L);
        black.setPHash(123456L);
        when(blacklistMapper.selectList(any())).thenReturn(List.of(black), List.<AudBlacklist>of());
        when(fileStorage.load(any())).thenAnswer(inv -> imageBytes());
        when(configService.getByKey("audit.pHashThreshold")).thenReturn(config("5"));

        service.checkBlacklist("a.png");
        service.removeBlacklist(5L);
        verify(blacklistMapper).deleteById(5L);
        service.checkBlacklist("b.png");

        // 失效后重新加载了一次，且拿到的是删除后的空列表
        verify(blacklistMapper, times(2)).selectList(any());
    }

    @Test
    void emptyBatchDoesNothing() {
        assertThat(service.batchAudit(List.of(), 1, "x", 9L)).isZero();
        verify(imageAuditMapper, never()).selectBatchIds(any());
    }

    private SysConfig config(String value) {
        SysConfig config = new SysConfig();
        config.setConfigKey("audit.pHashThreshold");
        config.setConfigValue(value);
        return config;
    }

    private InputStream imageBytes() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                image.getRaster().setSample(x, y, 0, Math.floorMod(x * 7 + y * 13, 256));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
