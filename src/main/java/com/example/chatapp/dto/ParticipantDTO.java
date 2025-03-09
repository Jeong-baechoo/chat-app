package com.example.chatapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ParticipantDTO {
    private Long userId;
    private String username;
    private Object role; // ParticipantRole enum
    private Object joinedAt; // LocalDateTime
}
