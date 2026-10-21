package com.orangecrave.admin.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.user.entity.SysChannel;
import com.orangecrave.admin.user.model.dto.SysChannelSaveDTO;
import com.orangecrave.admin.user.service.ChannelService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/ums/channel")
@RequiredArgsConstructor
public class UmsChannelController {

    private final ChannelService channelService;

    @SaCheckPermission("ums:channel:list")
    @GetMapping("/list")
    public Result<List<SysChannel>> list() {
        return Result.ok(channelService.list());
    }

    @SaCheckPermission("ums:channel:update")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody SysChannelSaveDTO dto) {
        dto.setId(null);
        channelService.save(dto);
        return Result.ok();
    }

    @SaCheckPermission("ums:channel:update")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysChannelSaveDTO dto) {
        channelService.save(dto);
        return Result.ok();
    }
}
