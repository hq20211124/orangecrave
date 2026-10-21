package com.orangecrave.admin.service.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.service.entity.SvcTicket;
import com.orangecrave.admin.service.entity.SvcTicketReply;
import com.orangecrave.admin.service.model.dto.SvcTicketSaveDTO;
import com.orangecrave.admin.service.service.SvcTicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/svc/ticket")
@RequiredArgsConstructor
public class SvcTicketController {

    private final SvcTicketService ticketService;

    @SaCheckPermission("svc:ticket:list")
    @GetMapping("/page")
    public Result<PageResult<SvcTicket>> page(PageQuery query,
                                              @RequestParam(required = false) Integer type,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(ticketService.page(query, type, status));
    }

    @GetMapping("/{id}")
    public Result<SvcTicket> get(@PathVariable Long id) {
        return Result.ok(ticketService.get(id));
    }

    @GetMapping("/{id}/replies")
    public Result<List<SvcTicketReply>> replies(@PathVariable Long id) {
        return Result.ok(ticketService.replies(id));
    }

    /** 客服代客建单。*/
    @SaCheckPermission("svc:ticket:update")
    @PostMapping
    public Result<SvcTicket> create(@Valid @RequestBody SvcTicketSaveDTO dto) {
        return Result.ok(ticketService.create(dto));
    }

    @SaCheckPermission("svc:ticket:reply")
    @PostMapping("/{id}/reply")
    public Result<SvcTicketReply> reply(@PathVariable Long id, @Valid @RequestBody ReplyDTO dto) {
        return Result.ok(ticketService.reply(id, 1, StpUtil.getLoginIdAsLong(), dto.getContent(), dto.getImages()));
    }

    @SaCheckPermission("svc:ticket:update")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        ticketService.updateStatus(id, status);
        return Result.ok();
    }

    @SaCheckPermission("svc:ticket:update")
    @PutMapping("/{id}/handler")
    public Result<Void> assign(@PathVariable Long id, @RequestParam Long handlerId) {
        ticketService.assign(id, handlerId);
        return Result.ok();
    }

    @SaCheckPermission("svc:ticket:close")
    @PostMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        ticketService.close(id);
        return Result.ok();
    }

    @Data
    public static class ReplyDTO {
        @NotBlank(message = "回复内容不能为空")
        @Size(max = 2000, message = "回复内容过长")
        private String content;
        private String images;
    }
}
