package com.example.chatapp.dto;

import com.example.chatapp.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String status;

    /**
     * User 엔티티를 UserDTO로 변환하는 정적 메서드
     */
    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .status(user.getStatus() != null ? user.getStatus().toString() : null)
                .build();
    }
}
