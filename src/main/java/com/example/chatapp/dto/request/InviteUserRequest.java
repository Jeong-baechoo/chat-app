package com.example.chatapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 초대 요청")
public class InviteUserRequest {
    
    @NotNull(message = "초대할 사용자 ID는 필수입니다")
    @Schema(description = "초대할 사용자 ID", example = "2")
    private Long userToInviteId;
}