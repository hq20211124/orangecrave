package com.orangecrave.admin.goods.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.goods.entity.PmsDesigner;
import com.orangecrave.admin.goods.model.dto.PmsDesignerSaveDTO;
import com.orangecrave.admin.goods.service.PmsDesignerService;
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
import java.util.Map;

@RestController
@RequestMapping("/admin-api/pms/designer")
@RequiredArgsConstructor
public class PmsDesignerController {

    private final PmsDesignerService service;

    @SaCheckPermission("goods:designer:list")
    @GetMapping("/page")
    public Result<PageResult<PmsDesigner>> page(PageQuery query) {
        return Result.ok(service.page(query));
    }

    @SaCheckPermission("goods:designer:list")
    @GetMapping("/list")
    public Result<List<PmsDesigner>> list() {
        return Result.ok(service.list());
    }

    /** 销量统计占位：只统计不分成（见 04 篇 §6） */
    @GetMapping("/{id}/sales")
    public Result<Map<String, Object>> sales(@PathVariable Long id) {
        return Result.ok(Map.of("designerId", id, "note", "v1 只做归属统计，出账线下"));
    }

    @SaCheckPermission("goods:designer:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PmsDesignerSaveDTO dto) {
        dto.setId(null);
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:designer:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody PmsDesignerSaveDTO dto) {
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:designer:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
