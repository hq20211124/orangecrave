package com.orangecrave.admin.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orangecrave.admin.finance.entity.RefundLedger;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface RefundLedgerMapper extends BaseMapper<RefundLedger> {

    /**
     * 复核结论（03 篇 §3.3 两段式）。只接受「待复核」的行，
     * 两名财务同时点通过时只有一次能改到行，退款不会执行两遍。 */
    @Update("UPDATE fin_refund_ledger SET review_status = #{reviewStatus}, status = #{status}, "
            + "reviewer_id = #{reviewerId}, review_remark = #{remark}, review_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND review_status = 1 AND deleted = 0")
    int markReviewed(@Param("id") Long id, @Param("reviewStatus") int reviewStatus,
                     @Param("status") int status, @Param("reviewerId") Long reviewerId,
                     @Param("remark") String remark);

    /** 退款执行成功后置为「成功」。 */
    @Update("UPDATE fin_refund_ledger SET status = 2, update_time = NOW() "
            + "WHERE id = #{id} AND status = 1 AND deleted = 0")
    int markExecuted(@Param("id") Long id);
}
