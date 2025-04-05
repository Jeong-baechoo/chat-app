package com.example.chatapp.controller;

import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.request.ChatRoomJoinRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
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

    @PostMapping
    public ResponseEntity<ChatRoomResponse> createChatRoom(@Valid @RequestBody ChatRoomCreateRequest request) {
        log.debug("채팅방 생성: name={}, type={}, creatorId={}", request.getName(), request.getType(), request.getCreatorId());
        try {
            return ResponseEntity.ok(chatRoomService.createChatRoom(request));
        } catch (Exception e) {
            log.warn("채팅방 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatRoomResponse>> getChatRoomsByUser(@PathVariable Long userId) {
        log.debug("사용자별 채팅방 조회: userId={}", userId);
        try {
            return ResponseEntity.ok(chatRoomService.findChatRoomsByUser(userId));
        } catch (Exception e) {
            log.warn("사용자별 채팅방 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ChatRoomResponse>> getAllChatRooms() {
        log.debug("전체 채팅방 조회");
        return ResponseEntity.ok(chatRoomService.findAllChatRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChatRoomResponse> getChatRoomById(@PathVariable Long id) {
        log.debug("채팅방 조회: id={}", id);
        return chatRoomService.findChatRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{chatRoomId}/join")
    public ResponseEntity<ChatRoomResponse> joinChatRoom(@PathVariable Long chatRoomId, @Valid @RequestBody ChatRoomJoinRequest request) {
        log.debug("채팅방 참여: chatRoomId={}, userId={}", chatRoomId, request.getUserId());
        try {
            return ResponseEntity.ok(chatRoomService.addParticipantToChatRoom(chatRoomId, request.getUserId()));
        } catch (Exception e) {
            log.warn("채팅방 참여 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable Long id) {
        log.debug("채팅방 삭제: id={}", id);
        try {
            chatRoomService.deleteChatRoom(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.warn("채팅방 삭제 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
