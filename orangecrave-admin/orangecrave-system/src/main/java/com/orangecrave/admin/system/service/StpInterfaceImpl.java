package com.orangecrave.admin.system.service;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orangecrave.admin.system.entity.SysRole;
import com.orangecrave.admin.system.entity.SysUserRole;
import com.orangecrave.admin.system.mapper.SysMenuMapper;
import com.orangecrave.admin.system.mapper.SysRoleMapper;
import com.orangecrave.admin.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 供给 Sa-Token 的权限/角色数据源：权限 = 用户所有角色关联菜单的 perm_key（见 02 篇 §1.4）。  */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        return menuMapper.selectPermKeysByUserId(userId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        List<SysUserRole> relations = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = relations.stream().map(SysUserRole::getRoleId).toList();
        return roleMapper.selectBatchIds(roleIds).stream().map(SysRole::getRoleKey).toList();
    }
}
