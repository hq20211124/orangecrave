package com.orangecrave.admin.marketing.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.marketing.entity.MktActivity;
import com.orangecrave.admin.marketing.model.dto.MktActivitySaveDTO;
import com.orangecrave.admin.marketing.service.MktActivityService;
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

@RestController
@RequestMapping("/admin-api/mkt/activity")
@RequiredArgsConstructor
public class MktActivityController {

    private final MktActivityService service;

    @SaCheckPermission("mkt:activity:list")
    @GetMapping("/page")
    public Result<PageResult<MktActivity>> page(PageQuery query, @RequestParam(required = false) Integer activityType) {
        return Result.ok(service.page(query, activityType));
    }

    @GetMapping("/{id}")
    public Result<MktActivity> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @SaCheckPermission("mkt:activity:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody MktActivitySaveDTO dto) {
        dto.setId(null);
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("mkt:activity:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody MktActivitySaveDTO dto) {
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("mkt:activity:status")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        service.updateStatus(id, status);
        return Result.ok();
    }

    @SaCheckPermission("mkt:activity:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
