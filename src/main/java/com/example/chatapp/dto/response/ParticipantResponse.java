package com.example.chatapp.dto.response;

import com.example.chatapp.domain.ParticipantRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "채팅방 참여자 정보 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponse {
    @Schema(description = "참여자 사용자 ID", example = "2")
    private Long userId;
    @Schema(description = "참여자 사용자명", example = "jane_smith")
    private String username;
    @Schema(description = "참여자 역할", example = "MEMBER")
    private ParticipantRole role;
    @Schema(description = "참여 시간", example = "2024-01-01T10:30:00")
    private LocalDateTime joinedAt;
}
