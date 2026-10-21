package com.orangecrave.admin.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orangecrave.admin.goods.entity.PmsSpecGroup;
import com.orangecrave.admin.goods.entity.PmsSpecValue;
import com.orangecrave.admin.goods.mapper.PmsSpecGroupMapper;
import com.orangecrave.admin.goods.mapper.PmsSpecValueMapper;
import com.orangecrave.admin.goods.model.dto.PmsSpecGroupSaveDTO;
import com.orangecrave.admin.goods.model.dto.PmsSpecValueSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PmsSpecService {

    private final PmsSpecGroupMapper groupMapper;
    private final PmsSpecValueMapper valueMapper;

    public List<PmsSpecGroup> groups(Long spuId) {
        return groupMapper.selectList(new LambdaQueryWrapper<PmsSpecGroup>()
                .eq(PmsSpecGroup::getSpuId, spuId).orderByAsc(PmsSpecGroup::getSort));
    }

    public List<PmsSpecValue> values(Long groupId) {
        return valueMapper.selectList(new LambdaQueryWrapper<PmsSpecValue>()
                .eq(PmsSpecValue::getGroupId, groupId).orderByAsc(PmsSpecValue::getSort));
    }

    public void saveGroup(PmsSpecGroupSaveDTO dto) {
        PmsSpecGroup group = new PmsSpecGroup();
        group.setId(dto.getId());
        group.setSpuId(dto.getSpuId());
        group.setGroupName(dto.getGroupName());
        group.setSort(dto.getSort());
        if (group.getId() == null) {
            groupMapper.insert(group);
        } else {
            groupMapper.updateById(group);
        }
    }

    public void deleteGroup(Long id) {
        groupMapper.deleteById(id);
    }

    public void saveValue(PmsSpecValueSaveDTO dto) {
        PmsSpecValue value = new PmsSpecValue();
        value.setId(dto.getId());
        value.setGroupId(dto.getGroupId());
        value.setValueName(dto.getValueName());
        value.setSort(dto.getSort());
        if (value.getId() == null) {
            valueMapper.insert(value);
        } else {
            valueMapper.updateById(value);
        }
    }

    public void deleteValue(Long id) {
        valueMapper.deleteById(id);
    }
}
