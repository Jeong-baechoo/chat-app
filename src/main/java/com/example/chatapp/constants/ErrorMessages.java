package com.example.chatapp.constants;

/**
 * 예외 메시지 상수 클래스
 * 예외 처리에서 사용되는 메시지를 중앙 집중식으로 관리합니다.
 */
public final class ErrorMessages {
    
    // 공통 메시지
    public static final String NOT_FOUND = "찾을 수 없습니다";
    public static final String ALREADY_EXISTS = "이미 존재하는";
    public static final String ALREADY_IN_USE = "이미 사용 중인";
    public static final String ACCESS_DENIED = "권한이 없습니다";
    
    // 사용자 관련 메시지
    public static final String USER_NOT_FOUND = "사용자를 찾을 수 없습니다";
    public static final String USERNAME_ALREADY_IN_USE = "이미 사용 중인 사용자명입니다";
    public static final String USER_ALREADY_EXISTS = "이미 존재하는 사용자입니다";
    
    // 채팅방 관련 메시지
    public static final String CHATROOM_NOT_FOUND = "채팅방을 찾을 수 없습니다";
    public static final String CHATROOM_ACCESS_DENIED = "채팅방에 대한 권한이 없습니다";
    
    // 메시지 관련 메시지
    public static final String MESSAGE_NOT_FOUND = "메시지를 찾을 수 없습니다";
    public static final String MESSAGE_ACCESS_DENIED = "메시지에 대한 권한이 없습니다";
    
    // 인증 관련 메시지
    public static final String UNAUTHORIZED = "인증되지 않은 사용자입니다";
    public static final String INVALID_CREDENTIALS = "잘못된 인증 정보입니다";
    
    private ErrorMessages() {
        // 유틸리티 클래스 - 인스턴스 생성 방지
        throw new UnsupportedOperationException("Utility class");
    }
}