package com.example.chatapp.domain.exception;

/**
 * 메시지 도메인 관련 예외
 */
public class MessageDomainException extends DomainException {
    public MessageDomainException(String message) {
        super(message);
    }

    public MessageDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}