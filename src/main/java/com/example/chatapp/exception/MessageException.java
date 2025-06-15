package com.example.chatapp.exception;

/**
 * 메시지 관련 비즈니스 예외
 */
public class MessageException extends BaseException {
    
    public MessageException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public MessageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public MessageException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    // 구체적인 예외 생성을 위한 정적 팩토리 메서드
    public static MessageException notFound(Long messageId) {
        return new MessageException(ErrorCode.MESSAGE_NOT_FOUND, 
            String.format("ID %d에 해당하는 메시지를 찾을 수 없습니다", messageId));
    }
    
    public static MessageException accessDenied() {
        return new MessageException(ErrorCode.MESSAGE_ACCESS_DENIED);
    }
    
    public static MessageException sendFailed(String reason) {
        return new MessageException(ErrorCode.MESSAGE_SEND_FAILED, 
            String.format("메시지 전송 실패: %s", reason));
    }
    
    public static MessageException invalidStatus(String status) {
        return new MessageException(ErrorCode.MESSAGE_INVALID_STATUS, 
            String.format("유효하지 않은 메시지 상태: %s", status));
    }
}
