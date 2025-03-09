package com.example.chatapp.dto;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    private Long id;
    private String name;
    private ChatRoomType type;
    private List<ParticipantDTO> participants;

    /**
     * ChatRoom 엔티티를 ChatRoomDTO로 변환하는 정적 메서드
     */
    public static ChatRoomDTO fromEntity(ChatRoom chatRoom) {
        List<ParticipantDTO> participantDTOs = null;

        if (chatRoom.getParticipants() != null) {
            participantDTOs = chatRoom.getParticipants().stream()
                    .map(participant -> ParticipantDTO.builder()
                            .userId(participant.getUser().getId())
                            .username(participant.getUser().getUsername())
                            .role(participant.getRole())
                            .joinedAt(participant.getJoinedAt())
                            .build())
                    .collect(Collectors.toList());
        }

        return ChatRoomDTO.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .type(chatRoom.getType())
                .participants(participantDTOs)
                .build();
    }
}
