package com.orangecrave.admin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.system.entity.SysRole;
import com.orangecrave.admin.system.model.dto.SysRoleDTO;
import com.orangecrave.admin.system.service.SysRoleService;
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
@RequestMapping("/admin-api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @SaCheckPermission("system:role:list")
    @GetMapping("/page")
    public Result<PageResult<SysRole>> page(PageQuery query) {
        return Result.ok(roleService.page(query));
    }

    @SaCheckPermission("system:role:list")
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.ok(roleService.list());
    }

    @SaCheckPermission("system:role:list")
    @GetMapping("/{id}/menus")
    public Result<List<Long>> menus(@PathVariable Long id) {
        return Result.ok(roleService.menuIds(id));
    }

    @SaCheckPermission("system:role:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody SysRoleDTO dto) {
        roleService.create(dto);
        return Result.ok();
    }

    @SaCheckPermission("system:role:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysRoleDTO dto) {
        roleService.update(dto);
        return Result.ok();
    }

    @SaCheckPermission("system:role:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok();
    }

    @SaCheckPermission("system:role:menus")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @Valid @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.ok();
    }
}
