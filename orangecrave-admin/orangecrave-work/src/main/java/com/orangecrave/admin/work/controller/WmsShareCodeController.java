package com.orangecrave.admin.work.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.work.entity.WmsShareCode;
import com.orangecrave.admin.work.service.WmsShareCodeService;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/admin-api/wms/share-code")
@RequiredArgsConstructor
public class WmsShareCodeController {

    private final WmsShareCodeService shareCodeService;

    @SaCheckPermission("wms:share-code:list")
    @GetMapping("/page")
    public Result<PageResult<WmsShareCode>> page(PageQuery query, @RequestParam(required = false) Integer shareType) {
        return Result.ok(shareCodeService.page(query, shareType));
    }

    @SaCheckPermission("wms:share-code:list")
    @GetMapping("/{id}")
    public Result<WmsShareCode> get(@PathVariable Long id) {
        return Result.ok(shareCodeService.get(id));
    }

    @SaCheckPermission("work:share-code:invalidate")
    @PutMapping("/{id}/invalidate")
    public Result<Void> invalidate(@PathVariable Long id, @RequestBody(required = false) ReasonDTO dto) {
        shareCodeService.invalidate(id, dto == null ? null : dto.getReason());
        return Result.ok();
    }

    @SaCheckPermission("work:share-code:refund")
    @PostMapping("/{id}/refund")
    public Result<BigDecimal> refund(@PathVariable Long id) {
        return Result.ok(shareCodeService.refundUnused(id));
    }

    @Data
    public static class ReasonDTO {
        @Size(max = 500, message = "原因过长")
        private String reason;
    }
}
