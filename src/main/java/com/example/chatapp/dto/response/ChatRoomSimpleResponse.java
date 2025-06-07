package com.example.chatapp.dto.response;

import com.example.chatapp.domain.ChatRoomType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
public class ChatRoomSimpleResponse {
    private Long id;
    private String name;
    private ChatRoomType type;

    public ChatRoomSimpleResponse(Long id, String name, ChatRoomType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
