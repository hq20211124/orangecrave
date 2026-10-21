package com.orangecrave.admin.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.orangecrave.admin.audit.entity.AudImageAudit;
import com.orangecrave.admin.audit.service.AuditService;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
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

import java.util.List;

@RestController
@RequestMapping("/admin-api/aud/image")
@RequiredArgsConstructor
public class AudImageController {

    private final AuditService auditService;

    /** 待审/审核记录分页（auto_result 筛选：1 通过 / 2 疑似 / 3 违规）。**/
    @SaCheckPermission("audit:image:list")
    @GetMapping("/page")
    public Result<PageResult<AudImageAudit>> page(PageQuery query, @RequestParam(required = false) Integer autoResult) {
        return Result.ok(auditService.page(query, autoResult));
    }

    @GetMapping("/{id}")
    public Result<AudImageAudit> get(@PathVariable Long id) {
        return Result.ok(auditService.get(id));
    }

    @SaCheckPermission("audit:image:check")
    @PostMapping("/check")
    public Result<AudImageAudit> check(@Valid @RequestBody CheckDTO dto) {
        return Result.ok(auditService.checkImage(dto.getImageKey(),
                dto.getBizType() == null ? 1 : dto.getBizType(), dto.getBizId()));
    }

    @SaCheckPermission("audit:image:approve")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) AuditDTO dto) {
        auditService.approve(id, StpUtil.getLoginIdAsLong(), dto == null ? null : dto.getRemark());
        return Result.ok();
    }

    @SaCheckPermission("audit:image:reject")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) AuditDTO dto) {
        auditService.reject(id, StpUtil.getLoginIdAsLong(), dto == null ? null : dto.getRemark());
        return Result.ok();
    }

    /** 批量审核（见 02 篇 §2.7）。result：1 通过 / 2 驳回。 */
    @SaCheckPermission("audit:image:batch")
    @PostMapping("/batch-audit")
    public Result<Integer> batchAudit(@Valid @RequestBody BatchAuditDTO dto) {
        int result = dto.getResult() == null ? 1 : dto.getResult();
        return Result.ok(auditService.batchAudit(dto.getIds(), result, dto.getRemark(), StpUtil.getLoginIdAsLong()));
    }

    @Data
    public static class CheckDTO {
        @NotBlank(message = "图片标识不能为空")
        private String imageKey;
        private Integer bizType;
        private Long bizId;
    }

    @Data
    public static class BatchAuditDTO {
        @NotEmpty(message = "请勾选要审核的图片")
        private List<Long> ids;
        /** 1 通过 / 2 驳回 */
        @Min(value = 1, message = "审核结果不合法")
        @Max(value = 2, message = "审核结果不合法")
        private Integer result;
        private String remark;
    }

    @Data
    public static class AuditDTO {
        @Size(max = 500, message = "备注过长")
        private String remark;
    }
}
