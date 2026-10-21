package com.orangecrave.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.system.entity.SysRole;
import com.orangecrave.admin.system.entity.SysRoleMenu;
import com.orangecrave.admin.system.mapper.SysRoleMapper;
import com.orangecrave.admin.system.mapper.SysRoleMenuMapper;
import com.orangecrave.admin.system.model.dto.SysRoleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色与授权（见 02 篇 §2.2）。  */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public PageResult<SysRole> page(PageQuery query) {
        Page<SysRole> page = roleMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SysRole>()
                .like(StringUtils.hasText(query.getKeyword()), SysRole::getRoleName, query.getKeyword())
                .orderByDesc(SysRole::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public List<SysRole> list() {
        return roleMapper.selectList(null);
    }

    public void create(SysRoleDTO dto) {
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setRemark(dto.getRemark());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        roleMapper.insert(role);
        saveMenus(role.getId(), dto.getMenuIds());
    }

    public void update(SysRoleDTO dto) {
        SysRole role = roleMapper.selectById(dto.getId());
        if (role == null) {
            throw new BizException("角色不存在");
        }
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            role.setStatus(dto.getStatus());
        }
        roleMapper.updateById(role);
    }

    public void delete(Long id) {
        roleMapper.deleteById(id);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
    }

    public List<Long> menuIds(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    @OperLog(module = "权限", type = "UPDATE", desc = "分配角色权限")
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        saveMenus(roleId, menuIds);
    }

    private void saveMenus(Long roleId, List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        for (Long menuId : menuIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
    }
}
