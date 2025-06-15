package com.example.chatapp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 인증 응답 DTO
 */
@Schema(description = "인증 결과 응답 정보를 담는 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    @Schema(description = "JWT 인증 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    @Schema(description = "사용자명", example = "john_doe")
    private String username;
    @Schema(description = "토큰 만료 시간", example = "2024-12-31T23:59:59.999Z")
    private Date expiresAt;
    @Schema(description = "토큰 유효성 여부", example = "true")
    private boolean valid;
}