package com.orangecrave.admin.goods.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.goods.entity.PmsSpecGroup;
import com.orangecrave.admin.goods.entity.PmsSpecValue;
import com.orangecrave.admin.goods.model.dto.PmsSpecGroupSaveDTO;
import com.orangecrave.admin.goods.model.dto.PmsSpecValueSaveDTO;
import com.orangecrave.admin.goods.service.PmsSpecService;
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
@RequestMapping("/admin-api/pms")
@RequiredArgsConstructor
public class PmsSpecController {

    private final PmsSpecService service;

    @GetMapping("/spec-group")
    public Result<List<PmsSpecGroup>> groups(@RequestParam Long spuId) {
        return Result.ok(service.groups(spuId));
    }

    @SaCheckPermission("goods:spec-group:update")
    @PostMapping("/spec-group")
    public Result<Void> saveGroup(@Valid @RequestBody PmsSpecGroupSaveDTO dto) {
        service.saveGroup(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:spec-group:update")
    @PutMapping("/spec-group")
    public Result<Void> updateGroup(@Valid @RequestBody PmsSpecGroupSaveDTO dto) {
        service.saveGroup(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:spec-group:update")
    @DeleteMapping("/spec-group/{id}")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        service.deleteGroup(id);
        return Result.ok();
    }

    @GetMapping("/spec-value")
    public Result<List<PmsSpecValue>> values(@RequestParam Long groupId) {
        return Result.ok(service.values(groupId));
    }

    @SaCheckPermission("goods:spec-value:update")
    @PostMapping("/spec-value")
    public Result<Void> saveValue(@Valid @RequestBody PmsSpecValueSaveDTO dto) {
        service.saveValue(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:spec-value:update")
    @PutMapping("/spec-value")
    public Result<Void> updateValue(@Valid @RequestBody PmsSpecValueSaveDTO dto) {
        service.saveValue(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:spec-value:update")
    @DeleteMapping("/spec-value/{id}")
    public Result<Void> deleteValue(@PathVariable Long id) {
        service.deleteValue(id);
        return Result.ok();
    }
}
