package com.example.chatapp.dto;

import com.example.chatapp.domain.User;
import com.example.chatapp.domain.UserStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Builder
public class UserDTO {
    private Long id;
    private String username;

    /**
     * User 엔티티를 UserDTO로 변환하는 정적 메서드
     */
    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();
    }
}
