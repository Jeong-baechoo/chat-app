package com.example.chatapp.controller;

import com.example.chatapp.dto.ChatRoomCreateDTO;
import com.example.chatapp.dto.ChatRoomDTO;
import com.example.chatapp.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;

    // 채팅방 생성
    @PostMapping
    public ResponseEntity<ChatRoomDTO> createChatRoom(@RequestBody ChatRoomCreateDTO chatRoomCreateDTO) {
        ChatRoomDTO createdChatRoom = chatRoomService.createChatRoom(chatRoomCreateDTO);
        return ResponseEntity.ok(createdChatRoom);
    }

    // 사용자가 참여한 채팅방 목록 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatRoomDTO>> getChatRoomsByUser(@PathVariable Long userId) {
        List<ChatRoomDTO> chatRooms = chatRoomService.findChatRoomsByUser(userId);
        return ResponseEntity.ok(chatRooms);
    }

    // 모든 채팅방 목록 조회
    @GetMapping
    public ResponseEntity<List<ChatRoomDTO>> getAllChatRooms() {
        List<ChatRoomDTO> chatRooms = chatRoomService.findAllChatRooms();
        return ResponseEntity.ok(chatRooms);
    }

    // 특정 채팅방 조회
    @GetMapping("/{id}")
    public ResponseEntity<ChatRoomDTO> getChatRoomById(@PathVariable Long id) {
        return chatRoomService.findChatRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{chatRoomId}/join")
    public ResponseEntity<ChatRoomDTO> joinChatRoom(
            @PathVariable Long chatRoomId,
            @RequestParam Long userId) {  // @RequestParam 사용 중이면 URL 파라미터로 전송해야 함
        ChatRoomDTO chatRoom = chatRoomService.addParticipantToChatRoom(chatRoomId, userId);
        return ResponseEntity.ok(chatRoom);
    }

    // 채팅방 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable Long id) {
        chatRoomService.deleteChatRoom(id);
        return ResponseEntity.noContent().build();
    }
}
