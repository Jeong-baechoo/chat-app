package com.example.chatapp.service;

import com.example.chatapp.domain.UserStatus;
import com.example.chatapp.dto.response.UserResponse;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<UserResponse> findAllUsers();
    UserResponse findUserById(Long id);
    Optional<UserResponse> findByUsername(String username);
    UserResponse updateUserStatus(Long id, UserStatus status);
    void deleteUser(Long id);

    List<UserResponse> findLoggedInUsers();
}
