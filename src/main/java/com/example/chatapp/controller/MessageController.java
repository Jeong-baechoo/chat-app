package com.example.chatapp.controller;

import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.request.MessageStatusUpdateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
@Slf4j
public class MessageController {
    private final MessageService messageService;

    /**
     * 메시지 전송
     */
    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody MessageCreateRequest request) {
        MessageResponse response = messageService.sendMessage(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 채팅방의 메시지 목록 조회 (페이지네이션)
     */
    @GetMapping("/chat-room/{chatRoomId}")
    public ResponseEntity<Page<MessageResponse>> getMessagesByChatRoom(
            @PathVariable Long chatRoomId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("채팅방 메시지 조회 API 요청: chatRoomId={}", chatRoomId);
        Page<MessageResponse> messages = messageService.getChatRoomMessages(chatRoomId, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * 특정 채팅방의 최근 메시지 조회 (제한된 개수)
     */
    @GetMapping("/chat-room/{chatRoomId}/recent")
    public ResponseEntity<List<MessageResponse>> getRecentMessagesByChatRoom(
            @PathVariable Long chatRoomId,
            @RequestParam(defaultValue = "50") int limit) {
        log.debug("채팅방 최근 메시지 조회 API 요청: chatRoomId={}, limit={}", chatRoomId, limit);
        List<MessageResponse> messages = messageService.getRecentChatRoomMessages(chatRoomId, limit);
        return ResponseEntity.ok(messages);
    }

    /**
     * 특정 메시지 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getMessageById(@PathVariable Long id) {
        log.debug("메시지 ID 조회 API 요청: id={}", id);
        try {
            MessageResponse message = messageService.findMessageById(id);
            return ResponseEntity.ok(message);
        } catch (MessageException e) {
            log.warn("메시지 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 메시지 상태 업데이트
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<MessageResponse> updateMessageStatus(
            @PathVariable Long id,
            @Valid @RequestBody MessageStatusUpdateRequest request) {
        log.debug("메시지 상태 업데이트 API 요청: id={}, status={}, userId={}",
                id, request.getStatus(), request.getUserId());
        try {
            MessageResponse updatedMessage = messageService.updateMessageStatus(
                    id, request.getUserId(), request.getStatus());
            return ResponseEntity.ok(updatedMessage);
        } catch (MessageException e) {
            log.warn("메시지 상태 업데이트 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * 특정 사용자가 보낸 메시지 조회
     */
    @GetMapping("/sender/{senderId}")
    public ResponseEntity<Page<MessageResponse>> getMessagesBySender(
            @PathVariable Long senderId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("발신자 메시지 조회 API 요청: senderId={}", senderId);
        Page<MessageResponse> messages = messageService.getMessagesBySender(senderId, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * 메시지 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long id,
            @RequestParam Long userId) {
        log.debug("메시지 삭제 API 요청: id={}, userId={}", id, userId);
        try {
            messageService.deleteMessage(id, userId);
            return ResponseEntity.noContent().build();
        } catch (MessageException e) {
            log.warn("메시지 삭제 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
