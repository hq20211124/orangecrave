package com.orangecrave.admin.work.service;

import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.text.SensitiveWordChecker;
import com.orangecrave.admin.work.entity.WmsCollection;
import com.orangecrave.admin.work.entity.WmsWork;
import com.orangecrave.admin.work.mapper.WmsCollectionItemMapper;
import com.orangecrave.admin.work.mapper.WmsCollectionMapper;
import com.orangecrave.admin.work.mapper.WmsWorkMapper;
import com.orangecrave.admin.work.model.dto.WmsCollectionSaveDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 作品合集成组规则（01 篇 §4：2–10 个作品）。
 * 数量校验在服务端做，客户端「至少两个 / 最多十个」的提示不是防线。 */
class WmsCollectionServiceTest {

    private WmsCollectionMapper collectionMapper;
    private WmsCollectionItemMapper itemMapper;
    private WmsWorkMapper workMapper;
    private WmsCollectionService service;

    @BeforeEach
    void setUp() {
        collectionMapper = mock(WmsCollectionMapper.class);
        itemMapper = mock(WmsCollectionItemMapper.class);
        workMapper = mock(WmsWorkMapper.class);
        SensitiveWordChecker wordChecker = mock(SensitiveWordChecker.class);
        when(wordChecker.check(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new WmsCollectionService(collectionMapper, itemMapper, workMapper, wordChecker);

        doAnswer(inv -> {
            ((WmsCollection) inv.getArgument(0)).setId(500L);
            return 1;
        }).when(collectionMapper).insert(any(WmsCollection.class));
    }

    @Test
    void savesTwoToTenWorks() {
        stubWorks(2);

        WmsCollection saved = service.save(dto(List.of(11L, 12L)));

        assertThat(saved.getId()).isEqualTo(500L);
        verify(itemMapper, times(2)).insert(any());
    }

    /** 只选一个不构成合集（01 篇 §4）。 */
    @Test
    void rejectsSingleWorkCollection() {
        assertThatThrownBy(() -> service.save(dto(List.of(11L))))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("至少选 2 个作品");
        verify(collectionMapper, never()).insert(any(WmsCollection.class));
    }

    /** 重复 id 先去掉再判上限，凑不出第 11 个作品。 */
    @Test
    void rejectsMoreThanTenWorks() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 11; i++) {
            ids.add(i);
        }

        assertThatThrownBy(() -> service.save(dto(ids)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("最多选 10 个作品");
    }

    /** 传了不存在的作品不能建出半截合集。 */
    @Test
    void rejectsUnknownWork() {
        when(workMapper.selectBatchIds(any())).thenReturn(List.of(work(11L)));

        assertThatThrownBy(() -> service.save(dto(List.of(11L, 999L))))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("作品不存在");
    }

    /** 重复 id 会被折叠，明细不会插两遍。 */
    @Test
    void deduplicatesWorkIds() {
        stubWorks(2);

        service.save(dto(List.of(11L, 11L, 12L)));

        ArgumentCaptor<com.orangecrave.admin.work.entity.WmsCollectionItem> item =
                ArgumentCaptor.forClass(com.orangecrave.admin.work.entity.WmsCollectionItem.class);
        verify(itemMapper, times(2)).insert(item.capture());
        assertThat(item.getAllValues()).extracting("workId").containsExactly(11L, 12L);
    }

    private void stubWorks(int count) {
        List<WmsWork> works = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            works.add(work(11L + i));
        }
        when(workMapper.selectBatchIds(any())).thenReturn(works);
    }

    private WmsWork work(Long id) {
        WmsWork work = new WmsWork();
        work.setId(id);
        return work;
    }

    private WmsCollectionSaveDTO dto(List<Long> workIds) {
        WmsCollectionSaveDTO dto = new WmsCollectionSaveDTO();
        dto.setUserId(9L);
        dto.setCollectionName("夏日无料合集");
        dto.setWorkIds(workIds);
        return dto;
    }
}
