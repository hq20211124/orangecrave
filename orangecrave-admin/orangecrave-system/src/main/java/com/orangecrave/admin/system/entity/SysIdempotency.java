package com.orangecrave.admin.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.orangecrave.admin.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 写操作幂等去重（见 02 篇 §1.5）。同一 (idem_key, api) 只执行一次，成功响应可重放。  */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_idempotency")
public class SysIdempotency extends BaseEntity {

    private String idemKey;
    /** METHOD + URI */
    private String api;
    /** 首次请求体 sha256，用于业务唯一键二次比对 */
    private String requestHash;
    private String responseBody;
    /** 1 处理中 / 2 已完成 / 3 失败可重试 */
    private Integer status;
}
