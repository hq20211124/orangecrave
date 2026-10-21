package com.orangecrave.admin.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.common.text.SensitiveWordChecker;
import com.orangecrave.admin.service.entity.SvcTicket;
import com.orangecrave.admin.service.entity.SvcTicketReply;
import com.orangecrave.admin.service.mapper.SvcTicketMapper;
import com.orangecrave.admin.service.mapper.SvcTicketReplyMapper;
import com.orangecrave.admin.service.model.dto.SvcTicketSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 客服工单（见 05 篇 §7）。四态流转：待受理 → 处理中 → 已解决 → 已关闭。
 * 侵权投诉走同一工单表（type=2）。 */
@Service
@RequiredArgsConstructor
public class SvcTicketService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SvcTicketMapper ticketMapper;
    private final SvcTicketReplyMapper replyMapper;
    private final SensitiveWordChecker wordChecker;

    public PageResult<SvcTicket> page(PageQuery query, Integer type, Integer status) {
        Page<SvcTicket> page = ticketMapper.selectPage(query.toPage(), new LambdaQueryWrapper<SvcTicket>()
                .eq(type != null, SvcTicket::getType, type)
                .eq(status != null, SvcTicket::getStatus, status)
                .orderByDesc(SvcTicket::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public SvcTicket get(Long id) {
        SvcTicket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new BizException("工单不存在");
        }
        return ticket;
    }

    public List<SvcTicketReply> replies(Long ticketId) {
        return replyMapper.selectList(new LambdaQueryWrapper<SvcTicketReply>()
                .eq(SvcTicketReply::getTicketId, ticketId).orderByAsc(SvcTicketReply::getId));
    }

    public SvcTicket create(SvcTicketSaveDTO dto) {
        SvcTicket ticket = new SvcTicket();
        ticket.setTicketNo("TK" + LocalDateTime.now().format(NO_FMT) + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        // 建单即待受理，后续流转走 updateStatus / assign / close
        ticket.setStatus(1);
        ticket.setUserId(dto.getUserId());
        ticket.setType(dto.getType());
        ticket.setOrderId(dto.getOrderId());
        ticket.setTitle(wordChecker.check(dto.getTitle()));
        ticket.setContent(wordChecker.check(dto.getContent()));
        ticket.setImages(dto.getImages());
        ticket.setSource(dto.getSource() == null ? 2 : dto.getSource());
        ticketMapper.insert(ticket);
        return ticket;
    }

    @Transactional(rollbackFor = Exception.class)
    public SvcTicketReply reply(Long ticketId, int replyType, Long userId, String content, String images) {
        get(ticketId);
        SvcTicketReply reply = new SvcTicketReply();
        reply.setTicketId(ticketId);
        reply.setReplyType(replyType);
        reply.setUserId(userId);
        reply.setContent(wordChecker.check(content));
        reply.setImages(images);
        replyMapper.insert(reply);
        // 客服回复即进入「处理中」
        SvcTicket ticket = get(ticketId);
        if (ticket.getStatus() != null && ticket.getStatus() == 1) {
            updateStatus(ticketId, 2);
        }
        return reply;
    }

    public void updateStatus(Long id, Integer status) {
        SvcTicket update = new SvcTicket();
        update.setId(id);
        update.setStatus(status);
        ticketMapper.updateById(update);
    }

    public void assign(Long id, Long handlerId) {
        SvcTicket update = new SvcTicket();
        update.setId(id);
        update.setHandlerId(handlerId);
        if (get(id).getStatus() != null && get(id).getStatus() == 1) {
            update.setStatus(2);
        }
        ticketMapper.updateById(update);
    }

    public void close(Long id) {
        updateStatus(id, 4);
    }
}
