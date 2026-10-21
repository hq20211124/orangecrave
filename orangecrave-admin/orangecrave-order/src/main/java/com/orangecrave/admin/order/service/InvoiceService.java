package com.orangecrave.admin.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.order.entity.OmsInvoice;
import com.orangecrave.admin.order.mapper.OmsInvoiceMapper;
import com.orangecrave.admin.order.model.OmsInvoiceSaveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 发票（见 01 篇 §5.6）。顾客端已上线的对外承诺，必须进 v1。  */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OmsInvoiceMapper invoiceMapper;

    public PageResult<OmsInvoice> page(PageQuery query) {
        Page<OmsInvoice> page = invoiceMapper.selectPage(query.toPage(), new LambdaQueryWrapper<OmsInvoice>()
                .orderByDesc(OmsInvoice::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public OmsInvoice get(Long id) {
        OmsInvoice invoice = invoiceMapper.selectById(id);
        if (invoice == null) {
            throw new BizException("发票不存在");
        }
        return invoice;
    }

    public void apply(OmsInvoiceSaveDTO dto) {
        OmsInvoice invoice = new OmsInvoice();
        invoice.setInvoiceNo("IV" + LocalDateTime.now().format(NO_FMT) + String.format("%04d", ThreadLocalRandom.current().nextInt(10000)));
        invoice.setUserId(dto.getUserId());
        invoice.setOrderIds(dto.getOrderIds());
        invoice.setTitleType(dto.getTitleType());
        invoice.setTitle(dto.getTitle());
        invoice.setTaxNo(dto.getTaxNo());
        invoice.setAmount(dto.getAmount());
        invoice.setEmail(dto.getEmail());
        invoice.setType(dto.getType());
        // 新申请一律待审，开票与驳回走专用端点
        invoice.setStatus(1);
        invoiceMapper.insert(invoice);
    }

    public void issue(Long id) {
        OmsInvoice update = new OmsInvoice();
        update.setId(id);
        update.setStatus(2);
        invoiceMapper.updateById(update);
    }

    public void reject(Long id) {
        OmsInvoice update = new OmsInvoice();
        update.setId(id);
        update.setStatus(3);
        invoiceMapper.updateById(update);
    }

    public void upload(Long id, String fileUrl) {
        OmsInvoice update = new OmsInvoice();
        update.setId(id);
        update.setFileUrl(fileUrl);
        invoiceMapper.updateById(update);
    }
}
