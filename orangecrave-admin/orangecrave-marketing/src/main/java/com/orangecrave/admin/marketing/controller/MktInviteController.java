package com.orangecrave.admin.marketing.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.marketing.entity.MktInvite;
import com.orangecrave.admin.marketing.service.MktInviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 邀请好友记录与拉新统计（见 02 篇 §2.8）。 */
@RestController
@RequestMapping("/admin-api/mkt/invite")
@RequiredArgsConstructor
public class MktInviteController {

    private final MktInviteService inviteService;

    @SaCheckPermission("mkt:invite:list")
    @GetMapping("/page")
    public Result<PageResult<MktInvite>> page(PageQuery query, @RequestParam(required = false) Long activityId) {
        return Result.ok(inviteService.page(query, activityId));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam(required = false) Long activityId) {
        return Result.ok(inviteService.statistics(activityId));
    }
}
