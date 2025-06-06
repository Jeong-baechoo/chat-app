package com.example.chatapp.controller;

import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.request.ChatRoomJoinRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.dto.response.ChatRoomSimpleResponse;
import com.example.chatapp.infrastructure.auth.AuthContext;
import com.example.chatapp.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
@Slf4j
public class ChatRoomController {
    private final ChatRoomService chatRoomService;
    private final AuthContext authContext;

    /**
     * 전체 채팅방 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<ChatRoomSimpleResponse>> getAllRooms() {
        log.debug("전체 채팅방 조회 API 요청");
        List<ChatRoomSimpleResponse> response = chatRoomService.findAllChatRoomsSimple();
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 사용자가 참여한 채팅방 목록 조회
     */
    @GetMapping("/me")
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms() {
        Long userId = authContext.getCurrentUserId();
        log.debug("사용자별 채팅방 조회 API 요청: userId={}", userId);
        List<ChatRoomResponse> response = chatRoomService.findChatRoomsByUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 채팅방 상세 정보 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChatRoomResponse> getRoomById(@PathVariable Long id) {
        log.debug("채팅방 상세 조회 API 요청: id={}", id);
        return chatRoomService.findChatRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 채팅방 생성
     */
    @PostMapping
    public ResponseEntity<ChatRoomResponse> createRoom(@Valid @RequestBody ChatRoomCreateRequest request) {
        Long userId = authContext.getCurrentUserId();
        log.debug("채팅방 생성 API 요청: name={}, type={}, userId={}",
                request.getName(), request.getType(), userId);

        // 인증된 사용자 ID를 creatorId로 설정
        request.setCreatorId(userId);

        ChatRoomResponse response = chatRoomService.createChatRoom(request);

        // 생성된 리소스의 URI를 헤더에 포함
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * 채팅방 참여
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<ChatRoomResponse> joinRoom(@PathVariable Long id) {
        Long userId = authContext.getCurrentUserId();
        log.debug("채팅방 참여 API 요청: roomId={}, userId={}", id, userId);
        ChatRoomResponse response = chatRoomService.addParticipantToChatRoom(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 채팅방 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        log.debug("채팅방 삭제 API 요청: id={}", id);
        Long currentUserId = authContext.getCurrentUserId();
        chatRoomService.deleteChatRoom(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
