package com.example.chatapp.dto;

import com.example.chatapp.domain.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ParticipantDTO {
    private Long userId;
    private String username;
    private ParticipantRole role; // ParticipantRole enum
    private LocalDateTime joinedAt; // LocalDateTime
}
