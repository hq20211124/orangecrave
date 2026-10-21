package com.orangecrave.admin.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.user.entity.UmsUser;
import com.orangecrave.admin.user.mapper.UmsUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 小程序用户（见 02 篇 §2.3）。  */
@Service
@RequiredArgsConstructor
public class UmsUserService {

    private final UmsUserMapper userMapper;

    public PageResult<UmsUser> page(PageQuery query, Integer identityType) {
        Page<UmsUser> page = userMapper.selectPage(query.toPage(), new LambdaQueryWrapper<UmsUser>()
                .eq(identityType != null, UmsUser::getIdentityType, identityType)
                .like(StringUtils.hasText(query.getKeyword()), UmsUser::getNickname, query.getKeyword())
                .or(StringUtils.hasText(query.getKeyword()))
                .like(StringUtils.hasText(query.getKeyword()), UmsUser::getPhone, query.getKeyword())
                .orderByDesc(UmsUser::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public UmsUser get(Long id) {
        UmsUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    public void updateStatus(Long id, Integer status) {
        UmsUser update = new UmsUser();
        update.setId(id);
        update.setCertStatus(status);
        userMapper.updateById(update);
    }

    /**
     * 顾客端登录桩（见 02 篇 §2.13）：由 code 换 openid（桩），不存在则新建用户。      */
    @Transactional(rollbackFor = Exception.class)
    public UmsUser wxLogin(String code, Long channelId) {
        if (!StringUtils.hasText(code)) {
            throw new BizException("code 不能为空");
        }
        String openid = "mock-" + code;
        UmsUser user = userMapper.selectOne(new LambdaQueryWrapper<UmsUser>().eq(UmsUser::getOpenid, openid));
        if (user == null) {
            user = new UmsUser();
            user.setOpenid(openid);
            user.setUnionid("union-" + code);
            user.setNickname("演示用户" + code);
            user.setIdentityType(0);
            user.setCertStatus(0);
            user.setChannelId(channelId);
            userMapper.insert(user);
        }
        return user;
    }
}
