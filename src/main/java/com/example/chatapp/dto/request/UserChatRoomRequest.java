package com.example.chatapp.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserChatRoomRequest {
    private Long userId;
    private Long chatRoomId;
}
