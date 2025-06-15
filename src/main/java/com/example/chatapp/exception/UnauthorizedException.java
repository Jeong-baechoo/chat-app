package com.example.chatapp.exception;

/**
 * 인증 관련 예외
 */
public class UnauthorizedException extends BaseException {
    
    public UnauthorizedException() {
        super(ErrorCode.AUTHENTICATION_FAILED);
    }
    
    public UnauthorizedException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, message);
    }
    
    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    // 구체적인 예외 생성을 위한 정적 팩토리 메서드
    public static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
    }
    
    public static UnauthorizedException invalidToken() {
        return new UnauthorizedException(ErrorCode.JWT_TOKEN_INVALID);
    }
    
    public static UnauthorizedException expiredToken() {
        return new UnauthorizedException(ErrorCode.JWT_TOKEN_EXPIRED);
    }
}
