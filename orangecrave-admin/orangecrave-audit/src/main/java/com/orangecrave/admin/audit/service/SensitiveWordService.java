package com.orangecrave.admin.audit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.audit.entity.AudSensitiveWord;
import com.orangecrave.admin.audit.mapper.AudSensitiveWordMapper;
import com.orangecrave.admin.audit.model.dto.AudSensitiveWordSaveDTO;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文本审核（见 05 篇 §2.3）。敏感词命中：替换 / 拒绝。  */
@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    private final AudSensitiveWordMapper mapper;

    public PageResult<AudSensitiveWord> page(PageQuery query) {
        Page<AudSensitiveWord> page = mapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<AudSensitiveWord>().orderByDesc(AudSensitiveWord::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public void save(AudSensitiveWordSaveDTO dto) {
        AudSensitiveWord word = new AudSensitiveWord();
        word.setId(dto.getId());
        word.setWord(dto.getWord());
        word.setCategory(dto.getCategory());
        word.setAction(dto.getAction());
        word.setStatus(dto.getStatus());
        if (word.getId() == null) {
            mapper.insert(word);
        } else {
            mapper.updateById(word);
        }
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }

    /** 返回命中的第一个敏感词；未命中返回 null。*/
    public AudSensitiveWord hit(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        List<AudSensitiveWord> words = mapper.selectList(new LambdaQueryWrapper<AudSensitiveWord>()
                .eq(AudSensitiveWord::getStatus, 1));
        for (AudSensitiveWord word : words) {
            if (word.getWord() != null && text.contains(word.getWord())) {
                return word;
            }
        }
        return null;
    }

    /** 命中「替换」动作时用 * 覆盖敏感词。*/
    public String mask(String text) {
        AudSensitiveWord hit = hit(text);
        if (hit == null) {
            return text;
        }
        if ("替换".equals(hit.getAction())) {
            return text.replace(hit.getWord(), "*".repeat(hit.getWord().length()));
        }
        return text;
    }
}
