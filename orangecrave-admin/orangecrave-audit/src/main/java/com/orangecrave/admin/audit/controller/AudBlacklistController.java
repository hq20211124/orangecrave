package com.orangecrave.admin.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.audit.entity.AudBlacklist;
import com.orangecrave.admin.audit.mapper.AudBlacklistMapper;
import com.orangecrave.admin.audit.service.AuditService;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin-api/aud/blacklist")
@RequiredArgsConstructor
public class AudBlacklistController {

    private final AudBlacklistMapper blacklistMapper;
    private final AuditService auditService;

    @SaCheckPermission("audit:blacklist:list")
    @GetMapping("/page")
    public Result<PageResult<AudBlacklist>> page(PageQuery query) {
        Page<AudBlacklist> page = blacklistMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<AudBlacklist>().orderByDesc(AudBlacklist::getId));
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @SaCheckPermission("audit:blacklist:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody BlacklistDTO dto) {
        auditService.addBlacklist(dto.getPHash(), dto.getSource(), dto.getRemark());
        return Result.ok();
    }

    @SaCheckPermission("audit:blacklist:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        auditService.removeBlacklist(id);
        return Result.ok();
    }

    /** 上传图片查 pHash 黑名单（见 02 篇 §2.7）。 */
    @SaCheckPermission("audit:blacklist:list")
    @PostMapping("/check")
    public Result<Map<String, Object>> check(@Valid @RequestBody CheckDTO dto) {
        return Result.ok(auditService.checkBlacklist(dto.getImageKey()));
    }

    @Data
    public static class BlacklistDTO {
        @NotNull(message = "图片哈希不能为空")
        private Long pHash;
        @Size(max = 50, message = "来源过长")
        private String source;
        @Size(max = 500, message = "备注过长")
        private String remark;
    }

    @Data
    public static class CheckDTO {
        @NotBlank(message = "图片标识不能为空")
        private String imageKey;
    }
}
