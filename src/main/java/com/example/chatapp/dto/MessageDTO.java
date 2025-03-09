package com.example.chatapp.dto;

import com.example.chatapp.domain.Message;
import com.example.chatapp.domain.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private String content;
    private UserDTO sender;
    private Long chatRoomId;
    private MessageStatus status;
    private LocalDateTime timestamp;

    public static MessageDTO fromEntity(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .sender(UserDTO.fromEntity(message.getSender()))
                .chatRoomId(message.getChatRoom().getId())
                .status(message.getStatus())
                .timestamp(message.getTimestamp())
                .build();
    }
}
