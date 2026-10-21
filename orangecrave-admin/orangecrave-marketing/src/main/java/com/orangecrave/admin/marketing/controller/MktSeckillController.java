package com.orangecrave.admin.marketing.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.marketing.entity.MktActivity;
import com.orangecrave.admin.marketing.model.dto.MktSeckillSaveDTO;
import com.orangecrave.admin.marketing.service.MktActivityService;
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

/**
 * 秒杀（见 02 篇 §2.8）。秒杀与拼团/优惠券/邀请共用 mkt_activity 骨架，
 * 靠 activity_type 区分；本控制器固定 activity_type = 2，规则存 rule_config。
 */
@RestController
@RequestMapping("/admin-api/mkt/seckill")
@RequiredArgsConstructor
public class MktSeckillController {

    /** 秒杀类型（见 01 篇 §7 rule_config）。 */
    private static final int TYPE_SECKILL = 2;

    private final MktActivityService activityService;

    @SaCheckPermission("mkt:seckill:list")
    @GetMapping("/page")
    public Result<PageResult<MktActivity>> page(PageQuery query) {
        return Result.ok(activityService.page(query, TYPE_SECKILL));
    }

    @GetMapping("/{id}")
    public Result<MktActivity> get(@PathVariable Long id) {
        return Result.ok(activityService.get(id));
    }

    @SaCheckPermission("mkt:seckill:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody MktSeckillSaveDTO dto) {
        dto.setId(null);
        activityService.saveSeckill(dto, TYPE_SECKILL);
        return Result.ok();
    }

    @SaCheckPermission("mkt:seckill:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody MktSeckillSaveDTO dto) {
        activityService.saveSeckill(dto, TYPE_SECKILL);
        return Result.ok();
    }

    @SaCheckPermission("mkt:seckill:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        activityService.delete(id);
        return Result.ok();
    }
}
