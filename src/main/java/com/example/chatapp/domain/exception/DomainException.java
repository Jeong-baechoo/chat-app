package com.example.chatapp.domain.exception;

/**
 * 도메인 레이어의 기본 예외 클래스
 * 모든 도메인 예외는 이 클래스를 상속해야 합니다.
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}