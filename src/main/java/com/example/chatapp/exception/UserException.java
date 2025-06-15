package com.example.chatapp.exception;

/**
 * 사용자 관련 비즈니스 예외
 */
public class UserException extends BaseException {
    
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public UserException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public UserException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    // 구체적인 예외 생성을 위한 정적 팩토리 메서드
    public static UserException notFound(Long userId) {
        return new UserException(ErrorCode.USER_NOT_FOUND, 
            String.format("ID %d에 해당하는 사용자를 찾을 수 없습니다", userId));
    }
    
    public static UserException notFound(String username) {
        return new UserException(ErrorCode.USER_NOT_FOUND, 
            String.format("사용자명 '%s'에 해당하는 사용자를 찾을 수 없습니다", username));
    }
    
    public static UserException alreadyExists(String username) {
        return new UserException(ErrorCode.USER_ALREADY_EXISTS, 
            String.format("사용자명 '%s'는 이미 사용 중입니다", username));
    }
    
    public static UserException accessDenied() {
        return new UserException(ErrorCode.USER_ACCESS_DENIED);
    }
}
