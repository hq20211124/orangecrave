package com.orangecrave.admin.user.controller;

import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.user.entity.UmsAddress;
import com.orangecrave.admin.user.service.UmsAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收货地址（只读，见 02 篇 §2.3）。 */
@RestController
@RequestMapping("/admin-api/ums/address")
@RequiredArgsConstructor
public class UmsAddressController {

    private final UmsAddressService addressService;

    @GetMapping("/page")
    public Result<PageResult<UmsAddress>> page(PageQuery query, @RequestParam(required = false) Long userId) {
        return Result.ok(addressService.page(query, userId));
    }
}
