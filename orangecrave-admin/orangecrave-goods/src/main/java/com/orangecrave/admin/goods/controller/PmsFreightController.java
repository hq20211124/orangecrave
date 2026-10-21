package com.orangecrave.admin.goods.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.goods.entity.PmsFreightTemplate;
import com.orangecrave.admin.goods.model.dto.PmsFreightTemplateSaveDTO;
import com.orangecrave.admin.goods.service.PmsFreightTemplateService;
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

@RestController
@RequestMapping("/admin-api/pms/freight")
@RequiredArgsConstructor
public class PmsFreightController {

    private final PmsFreightTemplateService service;

    @SaCheckPermission("goods:freight:list")
    @GetMapping("/page")
    public Result<PageResult<PmsFreightTemplate>> page(PageQuery query) {
        return Result.ok(service.page(query));
    }

    @SaCheckPermission("goods:freight:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PmsFreightTemplateSaveDTO dto) {
        dto.setId(null);
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:freight:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody PmsFreightTemplateSaveDTO dto) {
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:freight:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
