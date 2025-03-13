package com.example.chatapp.dto.response;

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
public class MessageResponse {
    private Long id;
    private String content;
    private UserResponse sender;
    private Long chatRoomId;
    private MessageStatus status;
    private LocalDateTime timestamp;
}
