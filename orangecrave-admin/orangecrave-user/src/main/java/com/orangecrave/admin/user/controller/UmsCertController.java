package com.orangecrave.admin.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.user.model.vo.UmsIdentityCertVO;
import com.orangecrave.admin.user.service.UmsCertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/ums/cert")
@RequiredArgsConstructor
public class UmsCertController {

    private final UmsCertService certService;

    @SaCheckPermission("ums:cert:list")
    @GetMapping("/page")
    public Result<PageResult<UmsIdentityCertVO>> page(PageQuery query, @RequestParam(required = false) Integer status) {
        return Result.ok(certService.page(query, status));
    }

    @SaCheckPermission("ums:cert:list")
    @GetMapping("/{id}")
    public Result<UmsIdentityCertVO> get(@PathVariable Long id) {
        return Result.ok(certService.get(id));
    }

    @SaCheckPermission("user:cert:approve")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) AuditDTO dto) {
        certService.approve(id, StpUtil.getLoginIdAsLong(), dto == null ? null : dto.getRemark());
        return Result.ok();
    }

    @SaCheckPermission("user:cert:reject")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody AuditDTO dto) {
        certService.reject(id, StpUtil.getLoginIdAsLong(), dto.getRemark());
        return Result.ok();
    }

    @Data
    public static class AuditDTO {
        @Size(max = 500, message = "驳回原因过长")
        private String remark;
    }
}
