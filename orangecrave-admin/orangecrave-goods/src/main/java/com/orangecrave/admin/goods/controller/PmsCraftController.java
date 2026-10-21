package com.orangecrave.admin.goods.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.goods.entity.PmsCraft;
import com.orangecrave.admin.goods.model.dto.PmsCraftSaveDTO;
import com.orangecrave.admin.goods.service.PmsCraftService;
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
@RequestMapping("/admin-api/pms/craft")
@RequiredArgsConstructor
public class PmsCraftController {

    private final PmsCraftService service;

    @SaCheckPermission("goods:craft:list")
    @GetMapping("/page")
    public Result<PageResult<PmsCraft>> page(PageQuery query) {
        return Result.ok(service.page(query));
    }

    @SaCheckPermission("goods:craft:list")
    @GetMapping("/list")
    public Result<List<PmsCraft>> list() {
        return Result.ok(service.list());
    }

    @SaCheckPermission("goods:craft:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PmsCraftSaveDTO dto) {
        dto.setId(null);
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:craft:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody PmsCraftSaveDTO dto) {
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:craft:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
