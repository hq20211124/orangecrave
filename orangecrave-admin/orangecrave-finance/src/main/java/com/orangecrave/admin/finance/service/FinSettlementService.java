package com.orangecrave.admin.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.finance.entity.FinDailySettlement;
import com.orangecrave.admin.finance.entity.RefundLedger;
import com.orangecrave.admin.finance.mapper.FinDailySettlementMapper;
import com.orangecrave.admin.finance.mapper.RefundLedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日结与对账（见 06 篇 §5.4）。
 * 对账口径：普通订单取支付时间，拼团订单取成团时间；退款记独立流水，不冲减销售额。 */
@Service
@RequiredArgsConstructor
public class FinSettlementService {

    private final FinDailySettlementMapper settlementMapper;
    private final RefundLedgerMapper refundLedgerMapper;

    public PageResult<FinDailySettlement> page(PageQuery query) {
        Page<FinDailySettlement> page = settlementMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<FinDailySettlement>().orderByDesc(FinDailySettlement::getSettleDate));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public PageResult<RefundLedger> refundPage(PageQuery query) {
        Page<RefundLedger> page = refundLedgerMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<RefundLedger>().orderByDesc(RefundLedger::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /** 退款流水导出（见 02 篇 §2.10）。v1 返回结构化 JSON，由前端生成文件。 */
    public List<RefundLedger> refundList() {
        return refundLedgerMapper.selectList(new LambdaQueryWrapper<RefundLedger>()
                .orderByDesc(RefundLedger::getId));
    }

    /** 对账明细：期望值（系统重算）vs 实际值（日结入库值）vs 差异。*/
    public List<Map<String, Object>> reconcile() {
        List<FinDailySettlement> settlements = settlementMapper.selectList(
                new LambdaQueryWrapper<FinDailySettlement>().orderByDesc(FinDailySettlement::getSettleDate));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FinDailySettlement settlement : settlements) {
            BigDecimal orderAmount = nvl(settlement.getOrderAmount());
            BigDecimal grouponAmount = nvl(settlement.getGrouponAmount());
            BigDecimal refundAmount = nvl(settlement.getRefundAmount());
            // 06 篇 §5.4 + 01 篇 §5.3 ④：普通单按支付口径，拼团单按成团口径，退款只记流水不冲减销售额
            BigDecimal expected = orderAmount.add(grouponAmount).subtract(refundAmount);
            BigDecimal actual = nvl(settlement.getNetAmount());
            Map<String, Object> row = new HashMap<>();
            row.put("settleDate", settlement.getSettleDate());
            row.put("expected", expected);
            row.put("actual", actual);
            row.put("diff", actual.subtract(expected));
            rows.add(row);
        }
        return rows;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
