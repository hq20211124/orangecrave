package com.orangecrave.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.orangecrave.admin.system.entity.SysIdempotency;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface SysIdempotencyMapper extends BaseMapper<SysIdempotency> {

    /**
     * 物理删除超期幂等记录（TTL 清理）。这里不能用逻辑删除：唯一键 uk_key_api 不含 deleted 列，
     * 被逻辑删掉的行仍会占住 (idem_key, api)，让同一个键从此无法再使用。故直接物理删。
     */
    @Delete("DELETE FROM sys_idempotency WHERE create_time < #{cutoff}")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
