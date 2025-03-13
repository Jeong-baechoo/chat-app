package com.example.chatapp.service;

import com.example.chatapp.domain.UserStatus;
import com.example.chatapp.dto.request.UserCreateRequest;
import com.example.chatapp.dto.response.UserResponse;

import java.util.List;
import java.util.Optional;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);
    List<UserResponse> findAllUsers();
    UserResponse findUserById(Long id);
    Optional<UserResponse> findByUsername(String username);
    UserResponse updateUserStatus(Long id, UserStatus status);
    void deleteUser(Long id);
}
