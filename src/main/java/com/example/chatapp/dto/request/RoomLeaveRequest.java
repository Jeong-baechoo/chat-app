package com.example.chatapp.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅방 퇴장 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomLeaveRequest {
    
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long roomId;
}