package com.orangecrave.admin.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.system.entity.SysConfig;
import com.orangecrave.admin.system.model.dto.SysConfigDTO;
import com.orangecrave.admin.system.service.SysConfigService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @SaCheckPermission("system:config:list")
    @GetMapping("/page")
    public Result<PageResult<SysConfig>> page(PageQuery query) {
        return Result.ok(configService.page(query));
    }

    @GetMapping("/{key}")
    public Result<SysConfig> getByKey(@PathVariable String key) {
        return Result.ok(configService.getByKey(key));
    }

    @SaCheckPermission("system:config:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysConfigDTO dto) {
        configService.update(dto);
        return Result.ok();
    }

    @SaCheckPermission("system:config:update")
    @PutMapping("/batch")
    public Result<Void> batchUpdate(@Valid @RequestBody List<SysConfigDTO> list) {
        configService.batchUpdate(list);
        return Result.ok();
    }
}
