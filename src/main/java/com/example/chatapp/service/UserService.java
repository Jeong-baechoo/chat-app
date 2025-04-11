package com.example.chatapp.service;

import com.example.chatapp.dto.response.UserResponse;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<UserResponse> findAllUsers();
    UserResponse findUserById(Long id);
    Optional<UserResponse> findByUsername(String username);
    void deleteUser(Long id);

    List<UserResponse> findLoggedInUsers();
}
