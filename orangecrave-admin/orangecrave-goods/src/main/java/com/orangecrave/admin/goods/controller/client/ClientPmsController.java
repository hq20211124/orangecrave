package com.orangecrave.admin.goods.controller.client;

import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.goods.entity.PmsCategory;
import com.orangecrave.admin.goods.entity.PmsSeries;
import com.orangecrave.admin.goods.entity.PmsSku;
import com.orangecrave.admin.goods.entity.PmsSpu;
import com.orangecrave.admin.goods.service.PmsCategoryService;
import com.orangecrave.admin.goods.service.PmsSeriesService;
import com.orangecrave.admin.goods.service.PmsSpuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客端商品只读接口（见 02 篇 §2.13）。  */
@RestController
@RequestMapping("/client-api/pms")
@RequiredArgsConstructor
public class ClientPmsController {

    private final PmsSeriesService seriesService;
    private final PmsCategoryService categoryService;
    private final PmsSpuService spuService;

    @GetMapping("/series/list")
    public Result<List<PmsSeries>> seriesList() {
        return Result.ok(seriesService.list());
    }

    @GetMapping("/category/tree")
    public Result<List<PmsCategory>> categoryTree() {
        return Result.ok(categoryService.tree());
    }

    @GetMapping("/spu/page")
    public Result<PageResult<PmsSpu>> spuPage(PageQuery query) {
        return Result.ok(spuService.page(query));
    }

    @GetMapping("/spu/{id}")
    public Result<PmsSpu> spuDetail(@PathVariable Long id) {
        return Result.ok(spuService.get(id));
    }

    @GetMapping("/spu/{id}/skus")
    public Result<List<PmsSku>> skus(@PathVariable Long id) {
        return Result.ok(spuService.skus(id));
    }
}
