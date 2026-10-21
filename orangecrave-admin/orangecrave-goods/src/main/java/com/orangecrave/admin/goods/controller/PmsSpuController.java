package com.orangecrave.admin.goods.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.goods.entity.PmsSku;
import com.orangecrave.admin.goods.entity.PmsSpu;
import com.orangecrave.admin.goods.model.dto.PmsSpuSaveDTO;
import com.orangecrave.admin.goods.service.PmsSpuService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.Data;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/admin-api/pms/spu")
@RequiredArgsConstructor
public class PmsSpuController {

    private final PmsSpuService service;

    @SaCheckPermission("goods:spu:list")
    @GetMapping("/page")
    public Result<PageResult<PmsSpu>> page(PageQuery query) {
        return Result.ok(service.page(query));
    }

    @GetMapping("/{id}")
    public Result<PmsSpu> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @GetMapping("/{id}/skus")
    public Result<List<PmsSku>> skus(@PathVariable Long id) {
        return Result.ok(service.skus(id));
    }

    @SaCheckPermission("goods:spu:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PmsSpuSaveDTO dto) {
        dto.setId(null);
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:spu:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody PmsSpuSaveDTO dto) {
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:spu:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    @SaCheckPermission("goods:spu:update")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        service.updateStatus(id, status);
        return Result.ok();
    }

    @SaCheckPermission("goods:price:update")
    @PutMapping("/{id}/price")
    public Result<Void> updatePrice(@PathVariable Long id, @Valid @RequestBody PriceDTO dto) {
        service.updatePrice(id, dto.getBasePrice(), dto.getAreaPrice());
        return Result.ok();
    }

    @Data
    public static class PriceDTO {
        @NotNull(message = "基础价不能为空")
        @DecimalMin(value = "0.00", message = "价格不能为负")
        private BigDecimal basePrice;
        @DecimalMin(value = "0.00", message = "价格不能为负")
        private BigDecimal areaPrice;
    }
}
