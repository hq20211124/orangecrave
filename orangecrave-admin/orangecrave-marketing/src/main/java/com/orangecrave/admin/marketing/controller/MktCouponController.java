package com.orangecrave.admin.marketing.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.marketing.entity.MktCoupon;
import com.orangecrave.admin.marketing.entity.MktCouponUser;
import com.orangecrave.admin.marketing.model.dto.MktCouponSaveDTO;
import com.orangecrave.admin.marketing.service.MktCouponService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

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
@RequestMapping("/admin-api/mkt/coupon")
@RequiredArgsConstructor
public class MktCouponController {

    private final MktCouponService service;

    @SaCheckPermission("mkt:coupon:list")
    @GetMapping("/page")
    public Result<PageResult<MktCoupon>> page(PageQuery query) {
        return Result.ok(service.page(query));
    }

    @SaCheckPermission("mkt:coupon:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody MktCouponSaveDTO dto) {
        dto.setId(null);
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("mkt:coupon:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody MktCouponSaveDTO dto) {
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("mkt:coupon:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    @SaCheckPermission("mkt:coupon:grant")
    @PostMapping("/{id}/grant")
    public Result<Integer> grant(@PathVariable Long id, @Valid @RequestBody List<Long> userIds) {
        return Result.ok(service.grant(id, userIds));
    }

    /** 批量发放（见 02 篇 §2.8）。 */
    @SaCheckPermission("mkt:coupon:grant")
    @PostMapping("/{id}/grant-batch")
    public Result<Integer> grantBatch(@PathVariable Long id, @Valid @RequestBody BatchGrantDTO dto) {
        return Result.ok(service.grant(id, dto.getUserIds()));
    }

    @GetMapping("/user/page")
    public Result<PageResult<MktCouponUser>> userPage(PageQuery query, @RequestParam(required = false) Long userId) {
        return Result.ok(service.userPage(query, userId));
    }

    @lombok.Data
    public static class BatchGrantDTO {
        @NotEmpty(message = "请至少选择一个用户")
        private List<Long> userIds;
    }
}
