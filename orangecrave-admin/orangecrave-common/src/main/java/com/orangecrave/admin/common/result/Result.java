package com.orangecrave.admin.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应信封：{ code, msg, data }，code == 0 为成功。与顾客端保持一致（见 02 篇 §1.2）。  */
@Data
public class Result<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        r.data = null;
        return r;
    }

    public static <T> Result<T> fail(ResultCode code, String msg) {
        return fail(code.getCode(), msg);
    }
}
