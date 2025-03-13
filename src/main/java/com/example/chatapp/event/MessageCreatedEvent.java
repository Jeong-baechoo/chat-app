package com.example.chatapp.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MessageCreatedEvent {
    private Long messageId;
    private Long senderId;
    private Long chatRoomId;
    private LocalDateTime timestamp;
}
