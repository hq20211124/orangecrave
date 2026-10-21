package com.orangecrave.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orangecrave.admin.system.entity.SysMenu;
import com.orangecrave.admin.system.mapper.SysMenuMapper;
import com.orangecrave.admin.system.model.dto.SysMenuDTO;
import com.orangecrave.admin.system.model.vo.MenuTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单与按钮权限（见 01 篇 §1）。  */
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;

    public List<MenuTreeVO> tree() {
        List<SysMenu> all = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort));
        return toTree(all);
    }

    public List<MenuTreeVO> toTree(List<SysMenu> menus) {
        List<MenuTreeVO> nodes = menus.stream().map(this::toVO).collect(Collectors.toList());
        List<MenuTreeVO> roots = new java.util.ArrayList<>();
        for (MenuTreeVO node : nodes) {
            if (node.getParentId() == null || node.getParentId() == 0) {
                roots.add(node);
            } else {
                nodes.stream()
                        .filter(p -> p.getId().equals(node.getParentId()))
                        .findFirst()
                        .ifPresent(p -> p.getChildren().add(node));
            }
        }
        return roots;
    }

    public void create(SysMenuDTO dto) {
        menuMapper.insert(toEntity(dto));
    }

    public void update(SysMenuDTO dto) {
        menuMapper.updateById(toEntity(dto));
    }

    public void delete(Long id) {
        menuMapper.deleteById(id);
    }

    private MenuTreeVO toVO(SysMenu m) {
        MenuTreeVO vo = new MenuTreeVO();
        vo.setId(m.getId());
        vo.setParentId(m.getParentId());
        vo.setMenuName(m.getMenuName());
        vo.setMenuType(m.getMenuType());
        vo.setPath(m.getPath());
        vo.setPermKey(m.getPermKey());
        vo.setIcon(m.getIcon());
        vo.setSort(m.getSort());
        return vo;
    }

    private SysMenu toEntity(SysMenuDTO dto) {
        SysMenu m = new SysMenu();
        m.setId(dto.getId());
        m.setParentId(dto.getParentId());
        m.setMenuName(dto.getMenuName());
        m.setMenuType(dto.getMenuType());
        m.setPath(dto.getPath());
        m.setPermKey(dto.getPermKey());
        m.setIcon(dto.getIcon());
        m.setSort(dto.getSort());
        return m;
    }
}
