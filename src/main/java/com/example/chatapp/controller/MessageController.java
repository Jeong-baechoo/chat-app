package com.example.chatapp.controller;

import com.example.chatapp.dto.request.MessageStatusUpdateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
@Slf4j
public class MessageController {
    private final MessageService messageService;

    /**
     * 채팅방 메시지 조회 (페이지네이션)
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<Page<MessageResponse>> findRoomMessages(
            @PathVariable Long roomId,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("채팅방 메시지 조회: roomId={}, page={}, size={}",
                roomId, pageable.getPageNumber(), pageable.getPageSize());
        Page<MessageResponse> messages = messageService.findChatRoomMessages(roomId, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * 채팅방 최근 메시지 조회
     */
    @GetMapping("/room/{roomId}/recent")
    public ResponseEntity<List<MessageResponse>> findRecentRoomMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "50") int limit) {
        log.debug("최근 메시지 조회: roomId={}, limit={}", roomId, limit);
        List<MessageResponse> messages = messageService.findRecentChatRoomMessages(roomId, limit);
        return ResponseEntity.ok(messages);
    }

    /**
     * 메시지 상태 업데이트 (읽음 표시 등)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<MessageResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody MessageStatusUpdateRequest request) {
        log.debug("메시지 상태 업데이트: id={}, status={}, userId={}", id, request.getStatus(), request.getUserId());
        MessageResponse updated = messageService.updateMessageStatus(id, request.getUserId(), request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
