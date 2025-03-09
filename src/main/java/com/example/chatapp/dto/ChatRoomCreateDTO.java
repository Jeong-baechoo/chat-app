package com.example.chatapp.dto;

import com.example.chatapp.domain.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomCreateDTO {
    private String name;
    private ChatRoomType type;
    private List<Long> participantIds;
    private Long creatorId;
}
