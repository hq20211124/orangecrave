package com.orangecrave.admin.goods.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.goods.entity.PmsTemplate;
import com.orangecrave.admin.goods.model.dto.PmsTemplateSaveDTO;
import com.orangecrave.admin.goods.service.PmsTemplateService;
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
@RequestMapping("/admin-api/pms/template")
@RequiredArgsConstructor
public class PmsTemplateController {

    private final PmsTemplateService service;

    @SaCheckPermission("goods:template:list")
    @GetMapping("/page")
    public Result<PageResult<PmsTemplate>> page(PageQuery query) {
        return Result.ok(service.page(query));
    }

    @SaCheckPermission("goods:template:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PmsTemplateSaveDTO dto) {
        dto.setId(null);
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:template:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody PmsTemplateSaveDTO dto) {
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:template:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
