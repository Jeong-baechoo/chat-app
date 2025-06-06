package com.example.chatapp.dto.response;

import com.example.chatapp.domain.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private Long id;
    private String name;
    private ChatRoomType type;
    private List<ParticipantResponse> participants;
    private LocalDateTime createdAt;
}
