package com.example.chatapp.dto.response;

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
public class ParticipantResponse {
    private Long userId;
    private String username;
    private ParticipantRole role;
    private LocalDateTime joinedAt;
}
