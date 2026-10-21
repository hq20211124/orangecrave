package com.orangecrave.admin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.system.entity.SysUser;
import com.orangecrave.admin.system.model.dto.SysUserDTO;
import com.orangecrave.admin.system.service.SysUserService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @SaCheckPermission("system:user:list")
    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(PageQuery query) {
        return Result.ok(userService.page(query));
    }

    @SaCheckPermission("system:user:list")
    @GetMapping("/{id}")
    public Result<SysUser> get(@PathVariable Long id) {
        return Result.ok(userService.get(id));
    }

    @SaCheckPermission("system:user:list")
    @GetMapping("/{id}/roles")
    public Result<List<Long>> roles(@PathVariable Long id) {
        return Result.ok(userService.roleIds(id));
    }

    @SaCheckPermission("system:user:save")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody SysUserDTO dto) {
        userService.create(dto);
        return Result.ok();
    }

    @SaCheckPermission("system:user:save")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysUserDTO dto) {
        userService.update(dto);
        return Result.ok();
    }

    @SaCheckPermission("system:user:save")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    @SaCheckPermission("system:user:save")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }

    @SaCheckPermission("system:user:password:reset")
    @PutMapping("/{id}/password/reset")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return Result.ok();
    }

    @SaCheckPermission("system:user:roles")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody List<Long> roleIds) {
        SysUserDTO dto = new SysUserDTO();
        dto.setId(id);
        dto.setRoleIds(roleIds);
        userService.update(dto);
        return Result.ok();
    }
}
