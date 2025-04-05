package com.example.chatapp.controller;

import com.example.chatapp.dto.request.UserCreateRequest;
import com.example.chatapp.dto.request.UserStatusUpdateRequest;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.service.UserService;
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
    private final UserService userService;
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.debug("사용자 생성: {}", request.getUsername());
        return ResponseEntity.ok(userService.createUser(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.debug("전체 사용자 조회");
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.debug("사용자 조회: id={}", id);
        try {
            return ResponseEntity.ok(userService.findUserById(id));
        } catch (Exception e) {
            log.warn("사용자 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        log.debug("사용자 조회: username={}", username);
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        log.debug("상태 업데이트: id={}, status={}", id, request.getStatus());
        return ResponseEntity.ok(userService.updateUserStatus(id, request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.debug("사용자 삭제: id={}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
