package com.example.chatapp.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "사용자 정보 응답 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long id;
    @Schema(description = "사용자명", example = "john_doe")
    private String username;
}
