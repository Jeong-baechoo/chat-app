// src/main/java/com/example/chatapp/service/MessageService.java
package com.example.chatapp.service;

import com.example.chatapp.domain.MessageStatus;
import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MessageService {
    MessageResponse sendMessage(MessageCreateRequest request);
    Page<MessageResponse> getChatRoomMessages(Long chatRoomId, Pageable pageable);
    List<MessageResponse> getRecentChatRoomMessages(Long chatRoomId, int limit);
    MessageResponse findMessageById(Long id);
    MessageResponse updateMessageStatus(Long messageId, Long userId, MessageStatus status);
    Page<MessageResponse> getMessagesBySender(Long senderId, Pageable pageable);
    void deleteMessage(Long id, Long userId);
}
