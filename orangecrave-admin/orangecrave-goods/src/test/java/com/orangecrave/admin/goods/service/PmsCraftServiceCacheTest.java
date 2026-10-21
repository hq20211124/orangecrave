package com.orangecrave.admin.goods.service;

import com.orangecrave.admin.common.cache.CacheProperties;
import com.orangecrave.admin.common.cache.LocalCacheFactory;
import com.orangecrave.admin.goods.entity.PmsCraft;
import com.orangecrave.admin.goods.mapper.PmsCraftMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 工艺字典缓存与失效。核心断言：反复取同一批工艺只回库一次（计价热路径依赖它），
 * 且 save/delete 后缓存必须失效，否则改了工艺价还算旧值。不依赖数据库。
 */
class PmsCraftServiceCacheTest {

    private PmsCraftMapper mapper;
    private PmsCraftService service;

    @BeforeEach
    void setUp() {
        mapper = Mockito.mock(PmsCraftMapper.class);
        service = new PmsCraftService(mapper, new LocalCacheFactory(new CacheProperties()));
    }

    @Test
    void repeatedListHitsCacheOnlyOnce() {
        when(mapper.selectList(any())).thenReturn(List.of(craft(1L)));

        assertThat(service.list()).hasSize(1);
        assertThat(service.list()).hasSize(1);

        verify(mapper, times(1)).selectList(any());
    }

    @Test
    void repeatedGetByIdsHitsCacheOnlyOnce() {
        when(mapper.selectById(2L)).thenReturn(craft(2L));

        service.getByIds(List.of(2L));
        service.getByIds(List.of(2L));

        verify(mapper, times(1)).selectById(2L);
    }

    @Test
    void deleteInvalidatesBothCaches() {
        when(mapper.selectList(any())).thenReturn(List.of(craft(1L)));
        when(mapper.selectById(2L)).thenReturn(craft(2L));

        service.list();
        service.getByIds(List.of(2L));
        service.delete(2L);
        service.list();
        service.getByIds(List.of(2L));

        verify(mapper, times(2)).selectList(any());
        verify(mapper, times(2)).selectById(2L);
    }

    private PmsCraft craft(Long id) {
        PmsCraft craft = new PmsCraft();
        craft.setId(id);
        craft.setCraftName("测试工艺");
        craft.setAddPrice(new BigDecimal("0.50"));
        craft.setStatus(1);
        return craft;
    }
}
