package com.example.chatapp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomJoinRequest {
    
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;
}
