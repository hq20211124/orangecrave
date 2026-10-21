package com.orangecrave.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.log.OperLogContext;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.system.entity.SysUser;
import com.orangecrave.admin.system.entity.SysUserRole;
import com.orangecrave.admin.system.mapper.SysUserMapper;
import com.orangecrave.admin.system.mapper.SysUserRoleMapper;
import com.orangecrave.admin.system.model.dto.SysUserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 管理员管理（见 02 篇 §2.2）。  */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public PageResult<SysUser> page(PageQuery query) {
        Page<SysUser> page = userMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(query.getKeyword()), SysUser::getUsername, query.getKeyword())
                .or(StringUtils.hasText(query.getKeyword()))
                .like(StringUtils.hasText(query.getKeyword()), SysUser::getNickname, query.getKeyword())
                .orderByDesc(SysUser::getId));
        page.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public SysUser get(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(SysUserDTO dto) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(
                StringUtils.hasText(dto.getPassword()) ? dto.getPassword() : "123456"));
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        userMapper.insert(user);
        saveRoles(user.getId(), dto.getRoleIds());
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserDTO dto) {
        SysUser user = userMapper.selectById(dto.getId());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        userMapper.updateById(user);
        if (dto.getRoleIds() != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId()));
            saveRoles(user.getId(), dto.getRoleIds());
        }
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
    }

    public void updateStatus(Long id, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @OperLog(module = "管理员", type = "UPDATE", desc = "重置密码")
    public void resetPassword(Long id, String newPassword) {
        SysUser before = userMapper.selectById(id);
        if (before == null) {
            throw new BizException("用户不存在");
        }
        OperLogContext.setBefore("{\"username\":\"" + before.getUsername() + "\"}");
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
        OperLogContext.setAfter("{\"username\":\"" + before.getUsername() + "\",\"password\":\"******\"}");
    }

    public List<Long> roleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }

    private void saveRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }
}
