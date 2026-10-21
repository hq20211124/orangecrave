package com.orangecrave.admin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.system.model.dto.SysMenuDTO;
import com.orangecrave.admin.system.model.vo.MenuTreeVO;
import com.orangecrave.admin.system.service.SysMenuService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @SaCheckPermission("system:menu:list")
    @GetMapping("/tree")
    public Result<List<MenuTreeVO>> tree() {
        return Result.ok(menuService.tree());
    }

    @SaCheckPermission("system:menu:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody SysMenuDTO dto) {
        menuService.create(dto);
        return Result.ok();
    }

    @SaCheckPermission("system:menu:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysMenuDTO dto) {
        menuService.update(dto);
        return Result.ok();
    }

    @SaCheckPermission("system:menu:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok();
    }
}
