package com.orangecrave.admin.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orangecrave.admin.common.cache.CacheProperties;
import com.orangecrave.admin.common.cache.LocalCacheFactory;
import com.orangecrave.admin.system.entity.SysConfig;
import com.orangecrave.admin.system.mapper.SysConfigMapper;
import com.orangecrave.admin.system.model.dto.SysConfigDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 系统参数缓存与失效。核心断言：反复读同一 key 只回库一次；写路径必须让缓存失效，
 * 否则改了配置读到的还是旧值（这是加缓存最容易留下的坑）。不依赖数据库。
 */
class SysConfigServiceCacheTest {

    private SysConfigMapper mapper;
    private SysConfigService service;

    @BeforeEach
    void setUp() {
        mapper = Mockito.mock(SysConfigMapper.class);
        service = new SysConfigService(mapper, new ObjectMapper(), new LocalCacheFactory(new CacheProperties()));
    }

    @Test
    void repeatedGetByKeyHitsCacheOnlyOnce() {
        when(mapper.selectOne(any())).thenReturn(config("orderUnpaidMinutes", "30"));

        assertThat(service.getByKey("orderUnpaidMinutes").getConfigValue()).isEqualTo("30");
        assertThat(service.getByKey("orderUnpaidMinutes").getConfigValue()).isEqualTo("30");

        verify(mapper, times(1)).selectOne(any());
    }

    @Test
    void updateInvalidatesCachedKey() {
        when(mapper.selectOne(any())).thenReturn(config("orderUnpaidMinutes", "30"));
        when(mapper.selectById(1L)).thenReturn(config("orderUnpaidMinutes", "30"));

        service.getByKey("orderUnpaidMinutes");

        SysConfigDTO dto = new SysConfigDTO();
        dto.setId(1L);
        dto.setConfigValue("45");
        service.update(dto);

        // 失效后再次读取必须回库（否则返回的是改前的旧值）
        service.getByKey("orderUnpaidMinutes");
        verify(mapper, times(2)).selectOne(any());
    }

    @Test
    void missingKeyIsNotCached() {
        when(mapper.selectOne(any())).thenReturn(null);

        try {
            service.getByKey("nope");
        } catch (Exception ignored) {
            // 配置项不存在会抛 404，这里只关心「查无」不被固化
        }
        try {
            service.getByKey("nope");
        } catch (Exception ignored) {
        }
        verify(mapper, times(2)).selectOne(any());
    }

    private SysConfig config(String key, String value) {
        SysConfig config = new SysConfig();
        config.setId(1L);
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType(1);
        return config;
    }
}
