package com.orangecrave.admin.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.user.entity.UmsAddress;
import com.orangecrave.admin.user.mapper.UmsAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 收货地址查询（见 02 篇 §2.3）。
 * 后台只读，用于客服核对；订单已存地址快照，后台不修改用户地址。 */
@Service
@RequiredArgsConstructor
public class UmsAddressService {

    private final UmsAddressMapper addressMapper;

    public PageResult<UmsAddress> page(PageQuery query, Long userId) {
        Page<UmsAddress> page = addressMapper.selectPage(query.toPage(), new LambdaQueryWrapper<UmsAddress>()
                .eq(userId != null, UmsAddress::getUserId, userId)
                .orderByDesc(UmsAddress::getIsDefault)
                .orderByDesc(UmsAddress::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
