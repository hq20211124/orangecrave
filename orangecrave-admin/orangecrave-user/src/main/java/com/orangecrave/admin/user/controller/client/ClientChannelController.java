package com.orangecrave.admin.user.controller.client;

import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.user.service.ChannelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 渠道上报（见 02 篇 §2.13 / 07 篇 §4.3）。  */
@RestController
@RequestMapping("/client-api/channel")
@RequiredArgsConstructor
public class ClientChannelController {

    private final ChannelService channelService;

    @PostMapping("/visit")
    public Result<Map<String, Object>> visit(@Valid @RequestBody VisitDTO dto) {
        Long channelId = channelService.idByCode(dto.getChannelCode());
        return Result.ok(Map.of("channelId", channelId == null ? -1L : channelId));
    }

    @Data
    public static class VisitDTO {
        @NotBlank(message = "渠道编码不能为空")
        @Size(max = 64, message = "渠道编码过长")
        private String channelCode;
    }
}
