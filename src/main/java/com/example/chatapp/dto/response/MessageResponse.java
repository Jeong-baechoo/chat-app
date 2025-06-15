package com.example.chatapp.dto.response;

import com.example.chatapp.domain.MessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "메시지 정보 응답 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    @Schema(description = "메시지 ID", example = "123")
    private Long id;
    @Schema(description = "메시지 내용", example = "안녕하세요! 회의 시작하겠습니다.")
    private String content;
    @Schema(description = "메시지 발신자 정보")
    private UserResponse sender;
    @Schema(description = "채팅방 ID", example = "1")
    private Long chatRoomId;
    @Schema(description = "메시지 상태", example = "SENT")
    private MessageStatus status;
    @Schema(description = "메시지 전송 시간", example = "2024-01-01T10:00:00")
    private LocalDateTime timestamp;
}
