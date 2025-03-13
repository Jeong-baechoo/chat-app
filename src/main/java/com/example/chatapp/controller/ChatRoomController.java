// src/main/java/com/example/chatapp/controller/ChatRoomController.java
package com.example.chatapp.controller;

import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.request.ChatRoomJoinRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms")
@Slf4j
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    // 채팅방 생성
    @PostMapping
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @Valid @RequestBody ChatRoomCreateRequest request) {
        log.debug("채팅방 생성 API 요청: name={}, type={}, creatorId={}",
                request.getName(), request.getType(), request.getCreatorId());
        try {
            ChatRoomResponse response = chatRoomService.createChatRoom(request);
            return ResponseEntity.ok(response);
        } catch (UserException e) {
            log.warn("채팅방 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 사용자가 참여한 채팅방 목록 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatRoomResponse>> getChatRoomsByUser(
            @PathVariable Long userId) {
        log.debug("사용자별 채팅방 조회 API 요청: userId={}", userId);
        try {
            List<ChatRoomResponse> response = chatRoomService.findChatRoomsByUser(userId);
            return ResponseEntity.ok(response);
        } catch (UserException e) {
            log.warn("사용자별 채팅방 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 모든 채팅방 목록 조회
    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getAllChatRooms() {
        log.debug("전체 채팅방 조회 API 요청");
        List<ChatRoomResponse> response = chatRoomService.findAllChatRooms();
        return ResponseEntity.ok(response);
    }

    // 특정 채팅방 조회
    @GetMapping("/{id}")
    public ResponseEntity<ChatRoomResponse> getChatRoomById(@PathVariable Long id) {
        log.debug("채팅방 ID 조회 API 요청: id={}", id);
        return chatRoomService.findChatRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 채팅방 참여
    @PostMapping("/{chatRoomId}/join")
    public ResponseEntity<ChatRoomResponse> joinChatRoom(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatRoomJoinRequest request) {
        log.debug("채팅방 참여 API 요청: chatRoomId={}, userId={}", chatRoomId, request.getUserId());
        try {
            ChatRoomResponse response = chatRoomService.addParticipantToChatRoom(chatRoomId, request.getUserId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("채팅방 참여 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 채팅방 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable Long id) {
        log.debug("채팅방 삭제 API 요청: id={}", id);
        try {
            chatRoomService.deleteChatRoom(id);
            return ResponseEntity.noContent().build();
        } catch (ChatRoomException e) {
            log.warn("채팅방 삭제 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
