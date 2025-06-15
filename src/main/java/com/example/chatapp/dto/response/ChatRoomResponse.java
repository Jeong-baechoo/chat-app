package com.example.chatapp.dto.response;

import com.example.chatapp.domain.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "채팅방 정보 응답 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    @Schema(description = "채팅방 ID", example = "1")
    private Long id;
    @Schema(description = "채팅방 이름", example = "개발팀 회의방")
    private String name;
    @Schema(description = "채팅방 타입", example = "PUBLIC")
    private ChatRoomType type;
    @Schema(description = "채팅방 참여자 목록")
    private List<ParticipantResponse> participants;
    @Schema(description = "생성 시간", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;
}
