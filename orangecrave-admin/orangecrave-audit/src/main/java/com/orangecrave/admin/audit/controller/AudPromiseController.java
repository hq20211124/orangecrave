package com.orangecrave.admin.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.audit.entity.AudCopyrightPromise;
import com.orangecrave.admin.audit.service.AuditService;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/aud/promise")
@RequiredArgsConstructor
public class AudPromiseController {

    private final AuditService auditService;

    @SaCheckPermission("audit:promise:list")
    @GetMapping("/page")
    public Result<PageResult<AudCopyrightPromise>> page(PageQuery query) {
        return Result.ok(auditService.promisePage(query));
    }
}
