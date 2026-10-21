package com.orangecrave.admin.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.audit.entity.AudSensitiveWord;
import com.orangecrave.admin.audit.model.dto.AudSensitiveWordSaveDTO;
import com.orangecrave.admin.audit.service.SensitiveWordService;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
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
@RequestMapping("/admin-api/aud/sensitive-word")
@RequiredArgsConstructor
public class AudSensitiveWordController {

    private final SensitiveWordService service;

    @SaCheckPermission("audit:sensitive:list")
    @GetMapping("/page")
    public Result<PageResult<AudSensitiveWord>> page(PageQuery query) {
        return Result.ok(service.page(query));
    }

    @SaCheckPermission("audit:sensitive-word:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody AudSensitiveWordSaveDTO dto) {
        dto.setId(null);
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("audit:sensitive-word:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody AudSensitiveWordSaveDTO dto) {
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("audit:sensitive-word:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
