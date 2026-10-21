package com.orangecrave.admin.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.log.OperLog;
import com.orangecrave.admin.common.page.PageQuery;
import com.orangecrave.admin.common.page.PageResult;
import com.orangecrave.admin.user.adapter.IllegalOrderAdapter;
import com.orangecrave.admin.user.entity.UmsIdentityCert;
import com.orangecrave.admin.user.entity.UmsUser;
import com.orangecrave.admin.user.mapper.UmsIdentityCertMapper;
import com.orangecrave.admin.user.mapper.UmsUserMapper;
import com.orangecrave.admin.user.model.vo.UmsIdentityCertVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 团长/摊主身份认证（见 01 篇 §2 / 02 篇 §2.3）。通过 → 自动发打样券（v1 记标记，券发放留待与营销打通）。  */
@Service
@RequiredArgsConstructor
public class UmsCertService {

    private final UmsIdentityCertMapper certMapper;
    private final UmsUserMapper userMapper;
    private final IllegalOrderAdapter illegalOrderAdapter;

    public PageResult<UmsIdentityCertVO> page(PageQuery query, Integer status) {
        Page<UmsIdentityCert> page = certMapper.selectPage(query.toPage(), new LambdaQueryWrapper<UmsIdentityCert>()
                .eq(status != null, UmsIdentityCert::getStatus, status)
                .orderByDesc(UmsIdentityCert::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                page.getRecords().stream().map(UmsIdentityCertVO::of).toList());
    }

    /** 出参一律脱敏（02 篇 §2.3），内部流程要用原始记录走 {@link #require}。 */
    public UmsIdentityCertVO get(Long id) {
        return UmsIdentityCertVO.of(require(id));
    }

    public UmsIdentityCert require(Long id) {
        UmsIdentityCert cert = certMapper.selectById(id);
        if (cert == null) {
            throw new BizException("认证记录不存在");
        }
        return cert;
    }

    @Transactional(rollbackFor = Exception.class)
    @OperLog(module = "身份认证", type = "AUDIT", desc = "认证通过并发打样券")
    public void approve(Long id, Long operatorId, String remark) {
        UmsIdentityCert cert = require(id);
        UmsIdentityCert update = new UmsIdentityCert();
        update.setId(id);
        update.setStatus(2);
        update.setAuditUserId(operatorId);
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(remark);
        update.setProofCouponGranted(1);
        certMapper.updateById(update);

        UmsUser user = new UmsUser();
        user.setId(cert.getUserId());
        user.setIdentityType(cert.getIdentityType());
        user.setCertStatus(2);
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @OperLog(module = "身份认证", type = "AUDIT", desc = "认证驳回")
    public void reject(Long id, Long operatorId, String remark) {
        UmsIdentityCert cert = require(id);
        UmsIdentityCert update = new UmsIdentityCert();
        update.setId(id);
        update.setStatus(3);
        update.setAuditUserId(operatorId);
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(remark);
        certMapper.updateById(update);

        UmsUser user = new UmsUser();
        user.setId(cert.getUserId());
        user.setCertStatus(3);
        userMapper.updateById(user);
        // 03 篇 §2.1：认证驳回即失去开团资格，其未进生产的订单按非法单收口（状态 13）
        illegalOrderAdapter.cancelIllegalOrders(cert.getUserId(), "身份认证未通过");
    }
}
