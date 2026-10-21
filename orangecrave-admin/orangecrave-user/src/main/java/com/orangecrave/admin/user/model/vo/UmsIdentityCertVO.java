package com.orangecrave.admin.user.model.vo;

import com.orangecrave.admin.common.util.MaskUtil;
import com.orangecrave.admin.user.entity.UmsIdentityCert;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 认证记录出参（02 篇 §2.3：身份证号脱敏）。后台列表与详情只给掩码，密文也不该出现在响应里。 */
@Data
public class UmsIdentityCertVO {

    private Long id;
    private Long userId;
    /** 1 团长 / 2 摊主 */
    private Integer identityType;
    private String realName;
    /** 掩码后的证件号 */
    private String idCardNo;
    private String materialUrls;
    /** 1 待审 / 2 通过 / 3 驳回 */
    private Integer status;
    private Long auditUserId;
    private LocalDateTime auditTime;
    private String auditRemark;
    private Integer proofCouponGranted;
    private LocalDateTime createTime;

    public static UmsIdentityCertVO of(UmsIdentityCert cert) {
        if (cert == null) {
            return null;
        }
        UmsIdentityCertVO vo = new UmsIdentityCertVO();
        vo.setId(cert.getId());
        vo.setUserId(cert.getUserId());
        vo.setIdentityType(cert.getIdentityType());
        vo.setRealName(cert.getRealName());
        vo.setIdCardNo(MaskUtil.idCard(cert.getIdCardNo()));
        vo.setMaterialUrls(cert.getMaterialUrls());
        vo.setStatus(cert.getStatus());
        vo.setAuditUserId(cert.getAuditUserId());
        vo.setAuditTime(cert.getAuditTime());
        vo.setAuditRemark(cert.getAuditRemark());
        vo.setProofCouponGranted(cert.getProofCouponGranted());
        vo.setCreateTime(cert.getCreateTime());
        return vo;
    }
}
