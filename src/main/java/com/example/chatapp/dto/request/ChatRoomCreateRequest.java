package com.example.chatapp.dto.request;

import com.example.chatapp.domain.ChatRoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomCreateRequest {
    @NotBlank(message = "채팅방 이름은 필수입니다.")
    private String name;

    @NotNull(message = "채팅방 타입은 필수입니다.")
    private ChatRoomType type;

    private List<Long> participantIds;

    private Long creatorId;
}
