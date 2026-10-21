package com.orangecrave.admin.goods.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.goods.entity.PmsSeries;
import com.orangecrave.admin.goods.model.dto.PmsSeriesSaveDTO;
import com.orangecrave.admin.goods.service.PmsSeriesService;
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
@RequestMapping("/admin-api/pms/series")
@RequiredArgsConstructor
public class PmsSeriesController {

    private final PmsSeriesService service;

    @SaCheckPermission("goods:series:list")
    @GetMapping("/page")
    public Result<PageResult<PmsSeries>> page(PageQuery query) {
        return Result.ok(service.page(query));
    }

    @SaCheckPermission("goods:series:list")
    @GetMapping("/list")
    public Result<List<PmsSeries>> list() {
        return Result.ok(service.list());
    }

    @GetMapping("/{id}")
    public Result<PmsSeries> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @SaCheckPermission("goods:series:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PmsSeriesSaveDTO dto) {
        dto.setId(null);
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:series:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody PmsSeriesSaveDTO dto) {
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:series:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
