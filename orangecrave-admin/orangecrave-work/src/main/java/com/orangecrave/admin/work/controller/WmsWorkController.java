package com.orangecrave.admin.work.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.work.entity.WmsWork;
import com.orangecrave.admin.work.entity.WmsWorkImage;
import com.orangecrave.admin.work.service.WmsWorkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/wms/work")
@RequiredArgsConstructor
public class WmsWorkController {

    private final WmsWorkService workService;

    @SaCheckPermission("wms:work:list")
    @GetMapping("/page")
    public Result<PageResult<WmsWork>> page(PageQuery query, @RequestParam(required = false) Integer status) {
        return Result.ok(workService.page(query, status));
    }

    @SaCheckPermission("wms:work:list")
    @GetMapping("/{id}")
    public Result<WmsWork> get(@PathVariable Long id) {
        return Result.ok(workService.get(id));
    }

    @SaCheckPermission("wms:work:list")
    @GetMapping("/{id}/images")
    public Result<List<WmsWorkImage>> images(@PathVariable Long id) {
        return Result.ok(workService.images(id));
    }

    @SaCheckPermission("work:work:update")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        workService.updateStatus(id, dto.getStatus(), dto.getRemark());
        return Result.ok();
    }

    @SaCheckPermission("work:work:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workService.delete(id);
        return Result.ok();
    }

    @Data
    public static class StatusDTO {
        @NotNull(message = "目标状态不能为空")
        private Integer status;
        @Size(max = 500, message = "备注过长")
        private String remark;
    }
}
