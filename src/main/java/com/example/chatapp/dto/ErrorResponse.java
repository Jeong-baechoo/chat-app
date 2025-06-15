package com.example.chatapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "에러 응답 정보")
public class ErrorResponse {
    @Schema(description = "에러 코드", allowableValues = {
            "VALIDATION_ERROR", "INTERNAL_SERVER_ERROR",
            "AUTH_ERROR", "INVALID_CREDENTIALS", "JWT_TOKEN_INVALID", "JWT_TOKEN_EXPIRED",
            "USER_NOT_FOUND", "USER_CONFLICT", "USER_FORBIDDEN",
            "CHATROOM_NOT_FOUND", "CHATROOM_FORBIDDEN", "CHATROOM_ALREADY_JOINED", 
            "CHATROOM_NOT_PARTICIPANT", "CHATROOM_ADMIN_REQUIRED",
            "MESSAGE_NOT_FOUND", "MESSAGE_FORBIDDEN", "MESSAGE_SEND_FAILED", "MESSAGE_INVALID_STATUS",
            "DOMAIN_ERROR"
    })
    private String errorCode;
    
    @Schema(description = "HTTP 상태", allowableValues = {
            "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "NOT_FOUND", "CONFLICT", "INTERNAL_SERVER_ERROR"
    })
    private String status;
    
    @Schema(description = "에러 메시지")
    private String message;
    
    @Schema(description = "에러 발생 시간")
    private LocalDateTime timestamp;
    
    @Schema(description = "필드별 에러 정보 (유효성 검증 실패 시)")
    private List<FieldError> fieldErrors;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "필드별 에러 정보")
    public static class FieldError {
        @Schema(description = "에러가 발생한 필드명")
        private String field;
        
        @Schema(description = "입력된 값")
        private Object rejectedValue;
        
        @Schema(description = "에러 메시지")
        private String message;
    }
}
