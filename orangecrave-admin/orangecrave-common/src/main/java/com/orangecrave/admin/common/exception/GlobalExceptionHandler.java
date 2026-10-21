package com.orangecrave.admin.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。所有异常都转成统一响应体（见 02 篇 §1.2）。  */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.fail(ResultCode.UNAUTHORIZED, ResultCode.UNAUTHORIZED.getMsg());
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public Result<Void> handleNoPermission(Exception e) {
        return Result.fail(ResultCode.FORBIDDEN, ResultCode.FORBIDDEN.getMsg());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidation(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? ResultCode.BIZ_VALIDATION.getMsg() : fe.getDefaultMessage();
        return Result.fail(ResultCode.BIZ_VALIDATION, msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未处理异常", e);
        return Result.fail(ResultCode.SERVER_ERROR, e.getMessage());
    }
}
