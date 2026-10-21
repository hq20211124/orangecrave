package com.orangecrave.admin.goods.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.goods.entity.PmsCategory;
import com.orangecrave.admin.goods.model.dto.PmsCategorySaveDTO;
import com.orangecrave.admin.goods.service.PmsCategoryService;
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
@RequestMapping("/admin-api/pms/category")
@RequiredArgsConstructor
public class PmsCategoryController {

    private final PmsCategoryService service;

    @SaCheckPermission("goods:category:list")
    @GetMapping("/tree")
    public Result<List<PmsCategory>> tree() {
        return Result.ok(service.tree());
    }

    @SaCheckPermission("goods:category:list")
    @GetMapping("/list")
    public Result<List<PmsCategory>> list() {
        return Result.ok(service.list());
    }

    @GetMapping("/{id}")
    public Result<PmsCategory> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @SaCheckPermission("goods:category:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PmsCategorySaveDTO dto) {
        dto.setId(null);
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:category:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody PmsCategorySaveDTO dto) {
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:category:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
