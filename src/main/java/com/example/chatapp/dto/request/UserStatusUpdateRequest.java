package com.example.chatapp.dto.request;

import com.example.chatapp.domain.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusUpdateRequest {
    @NotNull(message = "상태는 필수입니다.")
    private UserStatus status;
}
