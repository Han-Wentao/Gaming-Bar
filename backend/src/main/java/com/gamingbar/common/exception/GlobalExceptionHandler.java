package com.gamingbar.common.exception;

import com.gamingbar.common.result.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException exception) {
        return ApiResponse.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class
    })
    public ApiResponse<Void> handleBadRequest(Exception exception) {
        return ApiResponse.failure(400, "参数不合法");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Void> handleNotFound(NoResourceFoundException exception) {
        return ApiResponse.failure(404, "资源不存在或已失效");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOther(Exception exception) {
        return ApiResponse.failure(500, "服务器内部错误");
    }
}
