package com.example.chatapp.dto.response;

import com.example.chatapp.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private UserStatus status;

    public UserResponse(long l, String testUser) {
        this.id = l;
        this.username = testUser;
    }
}
