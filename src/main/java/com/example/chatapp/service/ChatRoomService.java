package com.example.chatapp.service;

import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

public interface ChatRoomService {
    ChatRoomResponse createChatRoom(@Valid ChatRoomCreateRequest request);
    List<ChatRoomResponse> findAllChatRooms();
    Optional<ChatRoomResponse> findChatRoomById(Long id);
    List<ChatRoomResponse> findChatRoomsByUser(Long userId);
    ChatRoomResponse addParticipantToChatRoom(Long chatRoomId, Long userId);
    void deleteChatRoom(Long id);
}
