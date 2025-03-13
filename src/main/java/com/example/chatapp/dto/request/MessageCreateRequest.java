package com.example.chatapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageCreateRequest {
    @NotNull(message = "발신자 ID는 필수입니다.")
    private Long senderId;

    @NotNull(message = "채팅방 ID는 필수입니다.")
    private Long chatRoomId;

    @NotBlank(message = "메시지 내용은 필수입니다.")
    @Size(max = 1000, message = "메시지 길이는 1000자를 초과할 수 없습니다.")
    private String content;
}
