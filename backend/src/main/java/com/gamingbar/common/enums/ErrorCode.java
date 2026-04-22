package com.gamingbar.common.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {

    BAD_REQUEST(400, "参数不合法"),
    UNAUTHORIZED(401, "未登录或 token 无效"),
    FORBIDDEN(403, "没有权限执行该操作"),
    NOT_FOUND(404, "资源不存在或已失效"),
    CONFLICT(409, "请求冲突"),
    RATE_LIMITED(429, "请求过于频繁"),
    SERVER_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
