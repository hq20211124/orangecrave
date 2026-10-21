package com.orangecrave.admin.audit.controller.client;

import com.orangecrave.admin.audit.entity.AudImageAudit;
import com.orangecrave.admin.audit.service.AuditService;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.common.satoken.StpClientUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端上传即审（见 02 篇 §2.13 / 05 篇 §2.1）。pHash 黑名单真实比对 + 机器审核桩。  */
@RestController
@RequestMapping("/client-api/aud")
@RequiredArgsConstructor
public class ClientAuditController {

    private final AuditService auditService;

    @PostMapping("/image/check")
    public Result<AudImageAudit> check(@Valid @RequestBody CheckDTO dto) {
        // 记录版权承诺（见 07 篇 §4.2）
        if (dto.getPromiseVersion() != null) {
            auditService.recordPromise(StpClientUtil.getLoginIdAsLong(), 1, dto.getBizId(),
                    dto.getPromiseVersion(), null);
        }
        return Result.ok(auditService.checkImage(dto.getImageKey(), 1, dto.getBizId()));
    }

    @Data
    public static class CheckDTO {
        @NotBlank(message = "图片标识不能为空")
        private String imageKey;
        private Long bizId;
        @Size(max = 32, message = "承诺版本号过长")
        private String promiseVersion;
    }
}
