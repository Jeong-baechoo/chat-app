// src/main/java/com/example/chatapp/service/MessageService.java
package com.example.chatapp.service;

import com.example.chatapp.domain.MessageStatus;
import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 메시지 서비스 인터페이스
 * 메시지 관련 비즈니스 로직을 정의합니다.
 */
public interface MessageService {
    /**
     * 메시지 전송
     */
    MessageResponse sendMessage(MessageCreateRequest request, Long senderId);

    /**
     * 채팅방 메시지 조회 (페이징)
     */
    Page<MessageResponse> findChatRoomMessages(Long chatRoomId, Pageable pageable);

    /**
     * 채팅방의 최근 메시지 조회
     */
    List<MessageResponse> findRecentChatRoomMessages(Long chatRoomId, int limit);

    /**
     * 메시지 ID로 조회
     */
    MessageResponse findMessageById(Long id);

    /**
     * 메시지 상태 업데이트
     */
    MessageResponse updateMessageStatus(Long messageId, Long userId, MessageStatus status);

    /**
     * 발신자별 메시지 조회
     */
    Page<MessageResponse> findMessagesBySender(Long senderId, Pageable pageable);

    /**
     * 메시지 삭제
     */
    void deleteMessage(Long id, Long userId);
}
