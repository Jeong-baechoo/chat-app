// src/main/java/com/example/chatapp/controller/UserController.java
package com.example.chatapp.controller;

import com.example.chatapp.dto.request.UserCreateRequest;
import com.example.chatapp.dto.request.UserStatusUpdateRequest;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.service.impl.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    private final UserServiceImpl userServiceImpl;

    // 사용자 생성
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {
        log.debug("사용자 생성 API 요청: username={}", request.getUsername());
        UserResponse newUser = userServiceImpl.createUser(request);
        return ResponseEntity.ok(newUser);
    }

    // 모든 사용자 조회
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.debug("전체 사용자 조회 API 요청");
        List<UserResponse> allUsers = userServiceImpl.findAllUsers();
        return ResponseEntity.ok(allUsers);
    }

    // 특정 사용자 조회
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.debug("사용자 ID 조회 API 요청: id={}", id);
        try {
            UserResponse user = userServiceImpl.findUserById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.warn("사용자 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // 사용자명으로 조회
    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        log.debug("사용자명 조회 API 요청: username={}", username);
        return userServiceImpl.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 사용자 상태 업데이트
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        log.debug("사용자 상태 업데이트 API 요청: id={}, status={}", id, request.getStatus());
        UserResponse updatedUser = userServiceImpl.updateUserStatus(id, request.getStatus());
        return ResponseEntity.ok(updatedUser);
    }

    // 사용자 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.debug("사용자 삭제 API 요청: id={}", id);
        userServiceImpl.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
