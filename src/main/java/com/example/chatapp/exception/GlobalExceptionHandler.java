package com.example.chatapp.exception;

import com.example.chatapp.domain.exception.DomainException;
import com.example.chatapp.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 전역 예외 처리기
 * 모든 예외를 캐치하여 일관된 형식의 에러 응답으로 변환
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * BaseException 및 하위 클래스 처리 (UserException, ChatRoomException, MessageException, UnauthorizedException)
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        
        // HTTP 상태 코드에 따라 로그 레벨 결정
        if (errorCode.getHttpStatus().is4xxClientError()) {
            // 4xx 에러는 클라이언트 오류
            if (errorCode.getHttpStatus() == HttpStatus.NOT_FOUND) {
                // 404는 자주 발생하므로 DEBUG 레벨
                log.debug("Resource not found - Code: {}, Message: {}, Path: {}", 
                    errorCode.getCode(), e.getMessage(), request.getRequestURI());
            } else if (errorCode.getHttpStatus() == HttpStatus.UNAUTHORIZED) {
                // 401은 인증 실패이므로 INFO 레벨
                log.info("Authentication failed - Code: {}, Message: {}, Path: {}", 
                    errorCode.getCode(), e.getMessage(), request.getRequestURI());
            } else {
                // 나머지 4xx는 WARN 레벨
                log.warn("Client error - Code: {}, Message: {}, Path: {}", 
                    errorCode.getCode(), e.getMessage(), request.getRequestURI());
            }
        } else if (errorCode.getHttpStatus().is5xxServerError()) {
            // 5xx 에러는 서버 오류이므로 ERROR 레벨 (스택트레이스 포함)
            log.error("Server error - Code: {}, Message: {}, Path: {}", 
                errorCode.getCode(), e.getMessage(), request.getRequestURI(), e);
        } else {
            // 기타 상태는 INFO 레벨
            log.info("Business exception - Code: {}, Message: {}, Path: {}", 
                errorCode.getCode(), e.getMessage(), request.getRequestURI());
        }
        
        ErrorResponse error = ErrorResponse.builder()
            .errorCode(errorCode.getCode())
            .status(errorCode.getStatus())
            .message(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
            
        return new ResponseEntity<>(error, errorCode.getHttpStatus());
    }
    
    /**
     * 도메인 예외 처리
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e, HttpServletRequest request) {
        // DomainException은 BaseException을 상속하므로 위의 핸들러에서 처리됨
        // 하지만 명시적으로 도메인 예외임을 표시하기 위해 별도 로깅
        log.error("Domain rule violation - Message: {}, Path: {}", 
            e.getMessage(), request.getRequestURI(), e);
            
        return handleBaseException(e, request);
    }
    
    /**
     * Bean Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        log.warn("Validation failed - Path: {}, Errors: {}", 
            request.getRequestURI(), e.getBindingResult().getErrorCount());
        
        List<ErrorResponse.FieldError> fieldErrors = new ArrayList<>();
        
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            ErrorResponse.FieldError fieldError = ErrorResponse.FieldError.builder()
                .field(error.getField())
                .rejectedValue(error.getRejectedValue())
                .message(error.getDefaultMessage())
                .build();
            fieldErrors.add(fieldError);
            
            log.debug("Validation error - Field: {}, Value: {}, Message: {}", 
                error.getField(), error.getRejectedValue(), error.getDefaultMessage());
        }
        
        ErrorResponse error = ErrorResponse.builder()
            .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
            .status(ErrorCode.VALIDATION_ERROR.getStatus())
            .message(ErrorCode.VALIDATION_ERROR.getDefaultMessage())
            .timestamp(LocalDateTime.now())
            .fieldErrors(fieldErrors)
            .build();
            
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * IllegalArgumentException 처리
     * 도메인 서비스에서 발생하는 검증 오류를 적절한 ErrorCode로 매핑
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        
        // 메시지 기반으로 적절한 ErrorCode 결정
        ErrorCode errorCode = mapMessageToErrorCode(e.getMessage());
        HttpStatus status = errorCode.getHttpStatus();
        
        // HTTP 상태에 따른 로그 레벨 설정
        if (status.is4xxClientError()) {
            log.warn("Client error - Code: {}, Message: {}, Path: {}", 
                errorCode.getCode(), e.getMessage(), request.getRequestURI());
        } else {
            log.error("Server error - Code: {}, Message: {}, Path: {}", 
                errorCode.getCode(), e.getMessage(), request.getRequestURI(), e);
        }
        
        ErrorResponse error = ErrorResponse.builder()
            .errorCode(errorCode.getCode())
            .status(errorCode.getStatus())
            .message(e.getMessage())
            .timestamp(LocalDateTime.now())
            .build();
            
        return new ResponseEntity<>(error, errorCode.getHttpStatus());
    }
    
    /**
     * 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception e, HttpServletRequest request) {
        
        log.error("Unexpected error occurred - Type: {}, Message: {}, Path: {}", 
            e.getClass().getSimpleName(), e.getMessage(), request.getRequestURI(), e);
        
        ErrorResponse error = ErrorResponse.builder()
            .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
            .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
            .message(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage())
            .timestamp(LocalDateTime.now())
            .build();
            
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 메시지 기반으로 적절한 ErrorCode 매핑
     */
    private ErrorCode mapMessageToErrorCode(String message) {
        if (message == null) {
            return ErrorCode.VALIDATION_ERROR;
        }
        
        // 도메인 서비스에서 발생하는 메시지 패턴 매칭
        if (message.contains("초대 권한이 없습니다")) {
            return ErrorCode.CHATROOM_PERMISSION_DENIED;
        }
        if (message.contains("관리자 권한이 필요합니다")) {
            return ErrorCode.CHATROOM_ADMIN_REQUIRED;
        }
        if (message.contains("이미 채팅방에 참여한 사용자입니다")) {
            return ErrorCode.CHATROOM_ALREADY_JOINED;
        }
        if (message.contains("채팅방에 참여하지 않은 사용자입니다")) {
            return ErrorCode.CHATROOM_NOT_PARTICIPANT;
        }
        if (message.contains("채팅방 참여자 수가 한계에 도달했습니다")) {
            return ErrorCode.VALIDATION_ERROR;
        }
        
        // 기본값
        return ErrorCode.VALIDATION_ERROR;
    }
}