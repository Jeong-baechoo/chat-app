package com.example.chatapp.exception;

import lombok.Getter;

/**
 * 애플리케이션의 모든 비즈니스 예외의 기본 클래스
 * ErrorCode를 포함하여 일관된 에러 처리를 지원
 */
@Getter
public abstract class BaseException extends RuntimeException {
    private final ErrorCode errorCode;
    
    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }
    
    protected BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    protected BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}