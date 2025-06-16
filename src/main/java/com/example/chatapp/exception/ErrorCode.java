package com.example.chatapp.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전체의 에러 코드를 정의하는 Enum
 * 각 에러 코드는 고유한 코드명, HTTP 상태, 기본 메시지를 가짐
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통 에러
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "요청 데이터가 유효하지 않습니다"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),
    
    // 인증 관련 에러
    AUTHENTICATION_FAILED("AUTH_ERROR", HttpStatus.UNAUTHORIZED, "인증에 실패했습니다"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "사용자명 또는 비밀번호가 올바르지 않습니다"),
    JWT_TOKEN_INVALID("JWT_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    JWT_TOKEN_EXPIRED("JWT_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    
    // 사용자 관련 에러
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    USER_ALREADY_EXISTS("USER_CONFLICT", HttpStatus.CONFLICT, "이미 존재하는 사용자입니다"),
    USER_ACCESS_DENIED("USER_FORBIDDEN", HttpStatus.FORBIDDEN, "사용자 접근 권한이 없습니다"),
    
    // 채팅방 관련 에러
    CHATROOM_NOT_FOUND("CHATROOM_NOT_FOUND", HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다"),
    CHATROOM_ACCESS_DENIED("CHATROOM_FORBIDDEN", HttpStatus.FORBIDDEN, "채팅방 접근 권한이 없습니다"),
    CHATROOM_ALREADY_JOINED("CHATROOM_ALREADY_JOINED", HttpStatus.BAD_REQUEST, "이미 참여 중인 채팅방입니다"),
    CHATROOM_NOT_PARTICIPANT("CHATROOM_NOT_PARTICIPANT", HttpStatus.BAD_REQUEST, "채팅방 참여자가 아닙니다"),
    CHATROOM_ADMIN_REQUIRED("CHATROOM_ADMIN_REQUIRED", HttpStatus.FORBIDDEN, "채팅방 관리자 권한이 필요합니다"),
    CHATROOM_PERMISSION_DENIED("CHATROOM_PERMISSION_DENIED", HttpStatus.FORBIDDEN, "채팅방 초대 권한이 없습니다"),
    
    // 메시지 관련 에러
    MESSAGE_NOT_FOUND("MESSAGE_NOT_FOUND", HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다"),
    MESSAGE_ACCESS_DENIED("MESSAGE_FORBIDDEN", HttpStatus.FORBIDDEN, "메시지 접근 권한이 없습니다"),
    MESSAGE_SEND_FAILED("MESSAGE_SEND_FAILED", HttpStatus.BAD_REQUEST, "메시지 전송에 실패했습니다"),
    MESSAGE_INVALID_STATUS("MESSAGE_INVALID_STATUS", HttpStatus.BAD_REQUEST, "유효하지 않은 메시지 상태입니다"),
    
    // 도메인 규칙 위반
    DOMAIN_RULE_VIOLATION("DOMAIN_ERROR", HttpStatus.BAD_REQUEST, "비즈니스 규칙을 위반했습니다");
    
    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
    
    /**
     * HTTP 상태 문자열 반환 (ErrorResponse에서 사용)
     */
    public String getStatus() {
        return this.httpStatus.name();
    }
}