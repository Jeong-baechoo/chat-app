package com.example.chatapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "메시지 생성 요청 정보를 담는 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreateRequest {
    @Schema(description = "채팅방 ID", example = "1", required = true)
    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;

    @Schema(description = "메시지 내용 (최대 1000자)", example = "안녕하세요! 회의 시작하겠습니다.", required = true)
    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 1000, message = "메시지 길이는 1000자를 초과할 수 없습니다.")
    private String content;

}
