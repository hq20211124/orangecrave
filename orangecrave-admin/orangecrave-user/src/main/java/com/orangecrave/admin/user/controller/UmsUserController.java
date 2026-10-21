package com.orangecrave.admin.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.user.entity.UmsUser;
import com.orangecrave.admin.user.service.UmsUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/ums/user")
@RequiredArgsConstructor
public class UmsUserController {

    private final UmsUserService userService;

    @SaCheckPermission("ums:user:list")
    @GetMapping("/page")
    public Result<PageResult<UmsUser>> page(PageQuery query, @RequestParam(required = false) Integer identityType) {
        return Result.ok(userService.page(query, identityType));
    }

    @SaCheckPermission("ums:user:list")
    @GetMapping("/{id}")
    public Result<UmsUser> get(@PathVariable Long id) {
        return Result.ok(userService.get(id));
    }

    @SaCheckPermission("ums:user:update")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }
}
