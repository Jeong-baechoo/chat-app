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

    // JPQL에서 NEW 연산자로 직접 생성에 사용할 생성자
    public ChatRoomSimpleResponse(Long id, String name, ChatRoomType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
