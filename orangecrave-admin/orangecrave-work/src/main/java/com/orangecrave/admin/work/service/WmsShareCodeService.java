package com.orangecrave.admin.work.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.finance.service.FinRefundService;
import com.orangecrave.admin.goods.entity.PmsSpu;
import com.orangecrave.admin.goods.mapper.PmsSpuMapper;
import com.orangecrave.admin.work.entity.WmsShareCode;
import com.orangecrave.admin.work.entity.WmsWork;
import com.orangecrave.admin.work.mapper.WmsShareCodeMapper;
import com.orangecrave.admin.work.mapper.WmsWorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 分享码（见 01 篇 §4 / 03 篇 §7.3）。无料分享预付款可退未核销余额。  */
@Service
@RequiredArgsConstructor
public class WmsShareCodeService {

    /** 失效原因三因（01 篇 §4，文案本系统自拟） */
    public static final String REASON_EXPIRED = "分享码已超过有效期";
    public static final String REASON_OVER_LIMIT = "分享码已达下单上限";

    private final WmsShareCodeMapper shareCodeMapper;
    private final FinRefundService finRefundService;
    private final WmsWorkMapper workMapper;
    private final PmsSpuMapper spuMapper;

    public PageResult<WmsShareCode> page(PageQuery query, Integer shareType) {
        Page<WmsShareCode> page = shareCodeMapper.selectPage(query.toPage(), new LambdaQueryWrapper<WmsShareCode>()
                .eq(shareType != null, WmsShareCode::getShareType, shareType)
                .orderByDesc(WmsShareCode::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public WmsShareCode get(Long id) {
        WmsShareCode code = shareCodeMapper.selectById(id);
        if (code == null) {
            throw new BizException("分享码不存在");
        }
        return code;
    }

    /**
     * 发起分享（04 篇 §2.3：未达起订量的作品不能发起无料分享）。
     * 可领套数上限必须达到作品 SPU 的起订量，否则领取者永远拼不出一次合规订单；
     * 校验沉在 service 层，HTTP 端点待分享功能排期时接入。  */
    @Transactional(rollbackFor = Exception.class)
    public WmsShareCode create(Long userId, Long workId, int shareType, int maxOrderQty,
                               BigDecimal prepayAmount, LocalDateTime expireTime) {
        if (shareType != 1 && shareType != 2) {
            throw new BizException("暂不支持该分享类型");
        }
        if (maxOrderQty < 1) {
            throw new BizException("可领套数必须大于 0");
        }
        WmsWork work = workMapper.selectById(workId);
        if (work == null) {
            throw new BizException("作品不存在");
        }
        PmsSpu spu = spuMapper.selectById(work.getSpuId());
        if (spu == null) {
            throw new BizException("作品未关联商品");
        }
        int min = spu.getMinOrderQty() == null ? 1 : spu.getMinOrderQty();
        if (maxOrderQty < min) {
            throw new BizException("「" + spu.getSpuName() + "」起订量为 " + min + " 件，还差 " + (min - maxOrderQty) + " 件");
        }
        WmsShareCode code = new WmsShareCode();
        code.setShareCode(generateShareCode());
        code.setUserId(userId);
        code.setWorkId(workId);
        code.setShareType(shareType);
        // 无料分享由分享者预付商品费（payer_type=2），普通分享由领取者付（03 篇 §7.3）
        code.setPayerType(shareType == 2 ? 2 : 1);
        code.setMaxOrderQty(maxOrderQty);
        code.setUsedQty(0);
        code.setExpireTime(expireTime);
        code.setStatus(1);
        code.setPrepayAmount(prepayAmount == null ? BigDecimal.ZERO : prepayAmount);
        code.setPrepayUsed(BigDecimal.ZERO);
        code.setPrepayRefunded(BigDecimal.ZERO);
        shareCodeMapper.insert(code);
        return code;
    }

    /** 分享码取「SC + 8 位随机大写字母数字」，撞库重试（领取者要手输，必须唯一）。 */
    private String generateShareCode() {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder("SC");
            for (int i = 0; i < 8; i++) {
                sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
            }
            String candidate = sb.toString();
            Long exists = shareCodeMapper.selectCount(new LambdaQueryWrapper<WmsShareCode>()
                    .eq(WmsShareCode::getShareCode, candidate));
            if (exists == null || exists == 0) {
                return candidate;
            }
        }
        throw new BizException("分享码生成失败，请重试");
    }

    @OperLog(module = "分享码", type = "UPDATE", desc = "强制失效分享码")
    public void invalidate(Long id, String reason) {
        WmsShareCode code = get(id);
        WmsShareCode update = new WmsShareCode();
        update.setId(code.getId());
        update.setStatus(0);
        update.setInvalidReason(reason == null ? "分享码已超过有效期" : reason);
        shareCodeMapper.updateById(update);
    }

    /** 退回未核销预付款（见 01 篇 §5.3 ⑤ / 03 篇 §7.3）。*/
    @Transactional(rollbackFor = Exception.class)
    @OperLog(module = "分享码", type = "FORCE_REFUND", desc = "退回未核销预付款")
    public BigDecimal refundUnused(Long id) {
        WmsShareCode code = get(id);
        BigDecimal amount = nz(code.getPrepayAmount())
                .subtract(nz(code.getPrepayUsed()))
                .subtract(nz(code.getPrepayRefunded()));
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("无可退余额");
        }
        finRefundService.record(3, code.getId(), null, code.getUserId(), amount, "无料分享未核销退款");
        WmsShareCode update = new WmsShareCode();
        update.setId(code.getId());
        update.setPrepayRefunded(nz(code.getPrepayRefunded()).add(amount));
        shareCodeMapper.updateById(update);
        return amount;
    }

    /**
     * 领取者下单核销（01 篇 §5⑤ / 03 篇 §7.3）：used_qty 与 prepay_used 一起加。
     * 上限、有效期、状态三个条件都写在同一条 UPDATE 的 WHERE 里，抢不到行就是超量或已失效。
     *
     * @param units 领取套数
     * @return 核销前的分享码（调用方要用 payerType / shareType 决定订单类型与邮付口径）
     */
    @Transactional(rollbackFor = Exception.class)
    public WmsShareCode redeem(String shareCode, int units) {
        WmsShareCode code = shareCodeMapper.selectOne(new LambdaQueryWrapper<WmsShareCode>()
                .eq(WmsShareCode::getShareCode, shareCode));
        if (code == null) {
            throw new BizException("分享码不存在");
        }
        if (code.getStatus() == null || code.getStatus() != 1) {
            throw new BizException("分享码已失效：" + nvlReason(code.getInvalidReason()));
        }
        if (code.getExpireTime() != null && code.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(REASON_EXPIRED);
        }
        if (code.getMaxOrderQty() != null && nzInt(code.getUsedQty()) + units > code.getMaxOrderQty()) {
            throw new BizException(REASON_OVER_LIMIT);
        }
        BigDecimal amount = redeemAmount(code, units);
        if (shareCodeMapper.redeem(code.getId(), units, amount) != 1) {
            // 并发下被别人抢先占满，或状态在这一次之间变了
            throw new BizException("分享码刚被用完或已失效，请重新确认");
        }
        return code;
    }

    /** 到期失效 + 退未核销预付款（00 篇 §5）。*/
    @Transactional(rollbackFor = Exception.class)
    public void expire(Long id) {
        if (shareCodeMapper.markExpired(id, REASON_EXPIRED) != 1) {
            return;
        }
        WmsShareCode code = get(id);
        BigDecimal balance = nz(code.getPrepayAmount()).subtract(nz(code.getPrepayUsed())).subtract(nz(code.getPrepayRefunded()));
        if (balance.signum() > 0) {
            finRefundService.record(3, code.getId(), null, code.getUserId(), balance, "分享码到期，退未核销预付款");
            WmsShareCode update = new WmsShareCode();
            update.setId(id);
            update.setPrepayRefunded(nz(code.getPrepayRefunded()).add(balance));
            shareCodeMapper.updateById(update);
        }
    }

    /** 预付的「已核销」按套数分摊：prepay_amount × 本次套数 ÷ 可下单套数上限（01 篇 §5⑤）。 */
    private BigDecimal redeemAmount(WmsShareCode code, int units) {
        BigDecimal prepay = nz(code.getPrepayAmount());
        if (prepay.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        Integer max = code.getMaxOrderQty();
        if (max == null || max <= 0) {
            return prepay.subtract(nz(code.getPrepayUsed())).subtract(nz(code.getPrepayRefunded())).max(BigDecimal.ZERO);
        }
        BigDecimal perUnit = prepay.divide(BigDecimal.valueOf(max), 6, RoundingMode.HALF_UP);
        return perUnit.multiply(BigDecimal.valueOf(units))
                .setScale(2, RoundingMode.HALF_UP)
                .min(nz(code.getPrepayAmount()).subtract(nz(code.getPrepayUsed())).subtract(nz(code.getPrepayRefunded())))
                .max(BigDecimal.ZERO);
    }

    private String nvlReason(String reason) {
        return reason == null || reason.isBlank() ? "已失效" : reason;
    }

    private int nzInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
