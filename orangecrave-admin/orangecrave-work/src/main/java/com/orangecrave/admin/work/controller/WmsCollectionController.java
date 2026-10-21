package com.orangecrave.admin.work.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.work.entity.WmsCollection;
import com.orangecrave.admin.work.entity.WmsCollectionItem;
import com.orangecrave.admin.work.service.WmsCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/wms/collection")
@RequiredArgsConstructor
public class WmsCollectionController {

    private final WmsCollectionService collectionService;

    @SaCheckPermission("wms:collection:list")
    @GetMapping("/page")
    public Result<PageResult<WmsCollection>> page(PageQuery query) {
        return Result.ok(collectionService.page(query));
    }

    @SaCheckPermission("wms:collection:list")
    @GetMapping("/{id}")
    public Result<WmsCollection> get(@PathVariable Long id) {
        return Result.ok(collectionService.get(id));
    }

    @SaCheckPermission("wms:collection:list")
    @GetMapping("/{id}/items")
    public Result<List<WmsCollectionItem>> items(@PathVariable Long id) {
        return Result.ok(collectionService.items(id));
    }

    @SaCheckPermission("work:collection:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        collectionService.delete(id);
        return Result.ok();
    }
}
