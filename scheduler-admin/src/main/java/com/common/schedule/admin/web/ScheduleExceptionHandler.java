package com.common.schedule.admin.web;

import com.common.schedule.admin.model.ApiResult;
import com.common.schedule.admin.service.ScheduleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 统一异常 → ApiResult。业务校验/冲突 400，缺参 400，其余 500。
 */
@RestControllerAdvice
public class ScheduleExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ScheduleExceptionHandler.class);

    @ExceptionHandler(ScheduleException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(ScheduleException ex) {
        return ResponseEntity.badRequest().body(ApiResult.fail(40000, ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ApiResult.fail(40000, "缺少参数：" + ex.getParameterName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleBadBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiResult.fail(40000, "请求体格式非法"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResult.fail(40000, "参数类型非法：" + ex.getName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnknown(Exception ex) {
        log.error("unhandled error", ex);
        return ResponseEntity.internalServerError().body(ApiResult.fail("服务器内部错误"));
    }
}