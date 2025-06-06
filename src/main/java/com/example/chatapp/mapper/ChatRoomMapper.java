package com.example.chatapp.mapper;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.ChatRoomParticipant;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.dto.response.ParticipantResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChatRoomMapper {


    public ChatRoomResponse toResponse(ChatRoom chatRoom) {
        List<ParticipantResponse> participantResponses = null;

        if (chatRoom.getParticipants() != null) {
            participantResponses = chatRoom.getParticipants().stream()
                    .map(this::toParticipantResponse)
                    .collect(Collectors.toList());
        }

        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName().getValue()) // ChatRoomName에서 실제 값 추출
                .type(chatRoom.getType())
                .participants(participantResponses)
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }

    private ParticipantResponse toParticipantResponse(ChatRoomParticipant participant) {
        return ParticipantResponse.builder()
                .userId(participant.getUser().getId())
                .username(participant.getUser().getUsername())
                .role(participant.getRole())
                .joinedAt(participant.getJoinedAt())
                .build();
    }
}
