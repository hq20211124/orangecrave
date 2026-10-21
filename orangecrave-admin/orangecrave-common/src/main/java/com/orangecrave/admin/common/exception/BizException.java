package com.orangecrave.admin.common.exception;

import com.orangecrave.admin.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常。抛出后用 GlobalExceptionHandler 转换为统一响应体。 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String msg) {
        super(msg);
        this.code = ResultCode.BIZ_VALIDATION.getCode();
    }

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BizException(ResultCode resultCode, String msg) {
        super(msg);
        this.code = resultCode.getCode();
    }
}
