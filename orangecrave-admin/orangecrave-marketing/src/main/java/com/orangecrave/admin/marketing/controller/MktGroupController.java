package com.orangecrave.admin.marketing.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.marketing.entity.MktGroup;
import com.orangecrave.admin.marketing.entity.MktGroupMember;
import com.orangecrave.admin.marketing.service.MktGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/mkt/group")
@RequiredArgsConstructor
public class MktGroupController {

    private final MktGroupService groupService;

    @SaCheckPermission("mkt:group:list")
    @GetMapping("/page")
    public Result<PageResult<MktGroup>> page(PageQuery query, @RequestParam(required = false) Integer status) {
        return Result.ok(groupService.page(query, status));
    }

    @GetMapping("/{id}")
    public Result<MktGroup> get(@PathVariable Long id) {
        return Result.ok(groupService.get(id));
    }

    @GetMapping("/{id}/members")
    public Result<List<MktGroupMember>> members(@PathVariable Long id) {
        return Result.ok(groupService.members(id));
    }

    @SaCheckPermission("mkt:group:force")
    @PostMapping("/{id}/force-success")
    public Result<Void> forceSuccess(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean simulated) {
        groupService.forceSuccess(id, simulated);
        return Result.ok();
    }

    @SaCheckPermission("mkt:group:force")
    @PostMapping("/{id}/force-fail")
    public Result<Void> forceFail(@PathVariable Long id) {
        groupService.forceFail(id);
        return Result.ok();
    }
}
