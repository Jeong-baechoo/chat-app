package com.example.chatapp.domain.exception;

import com.example.chatapp.exception.BaseException;
import com.example.chatapp.exception.ErrorCode;

/**
 * 도메인 레이어의 기본 예외 클래스
 * 모든 도메인 예외는 이 클래스를 상속해야 합니다.
 */
public class DomainException extends BaseException {
    
    public DomainException(String message) {
        super(ErrorCode.DOMAIN_RULE_VIOLATION, message);
    }
    
    public DomainException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DomainException(String message, Throwable cause) {
        super(ErrorCode.DOMAIN_RULE_VIOLATION, message, cause);
    }
}