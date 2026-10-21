package com.orangecrave.admin.goods.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.goods.entity.PmsSku;
import com.orangecrave.admin.goods.model.dto.PmsSkuPriceDTO;
import com.orangecrave.admin.goods.model.dto.PmsSkuSaveDTO;
import com.orangecrave.admin.goods.service.PmsSkuService;
import jakarta.validation.Valid;

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
@RequestMapping("/admin-api/pms/sku")
@RequiredArgsConstructor
public class PmsSkuController {

    private final PmsSkuService service;

    @GetMapping("/list")
    public Result<List<PmsSku>> list(@RequestParam Long spuId) {
        return Result.ok(service.list(spuId));
    }

    @SaCheckPermission("goods:sku:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PmsSkuSaveDTO dto) {
        dto.setId(null);
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:sku:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody PmsSkuSaveDTO dto) {
        service.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("goods:sku:update")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }

    @SaCheckPermission("goods:stock:update")
    @PutMapping("/{id}/stock")
    public Result<Void> updateStock(@PathVariable Long id, @RequestParam Integer stock) {
        service.updateStock(id, stock);
        return Result.ok();
    }

    @SaCheckPermission("goods:price:batch")
    @PostMapping("/batch-price")
    public Result<Void> batchPrice(@Valid @RequestBody List<PmsSkuPriceDTO> items) {
        service.batchPrice(items);
        return Result.ok();
    }

    @SaCheckPermission("goods:price:update")
    @PutMapping("/{id}/price")
    public Result<Void> updatePrice(@PathVariable Long id, @RequestParam BigDecimal price) {
        service.updatePrice(id, price);
        return Result.ok();
    }
}
