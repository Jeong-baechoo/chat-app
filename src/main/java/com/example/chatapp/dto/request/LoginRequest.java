package com.example.chatapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 요청 DTO
 */
@Schema(description = "로그인 요청 정보를 담는 DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @Schema(description = "사용자명 (3-20자)", example = "john_doe", required = true)
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 20, message = "사용자명은 3-20자 사이여야 합니다")
    private String username;
    
    @Schema(description = "비밀번호 (6자 이상)", example = "password123", required = true)
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 6, max = 100, message = "비밀번호는 6자 이상이어야 합니다")
    private String password;
}