package com.orangecrave.admin.user.controller.client;

import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.common.satoken.StpClientUtil;
import com.orangecrave.admin.user.entity.UmsUser;
import com.orangecrave.admin.user.service.UmsUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 顾客端登录（见 02 篇 §2.13）。wx.login 的 code 换 token（v1 桩）。  */
@RestController
@RequestMapping("/client-api/auth")
@RequiredArgsConstructor
public class ClientAuthController {

    private final UmsUserService userService;

    @PostMapping("/wx-login")
    public Result<Map<String, Object>> wxLogin(@Valid @RequestBody WxLoginDTO dto) {
        UmsUser user = userService.wxLogin(dto.getCode(), dto.getChannelId());
        StpClientUtil.login(user.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("token", StpClientUtil.getTokenValue());
        data.put("tokenName", StpClientUtil.getTokenName());
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        return Result.ok(data);
    }

    @Data
    public static class WxLoginDTO {
        @NotBlank(message = "微信登录凭证不能为空")
        @Size(max = 64, message = "登录凭证过长")
        private String code;
        private Long channelId;
    }
}
