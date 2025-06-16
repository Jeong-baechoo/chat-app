package com.example.chatapp.controller;

import com.example.chatapp.config.WebFilterConfig;
import com.example.chatapp.domain.MessageStatus;
import com.example.chatapp.dto.request.MessageStatusUpdateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.GlobalExceptionHandler;
import com.example.chatapp.infrastructure.auth.AuthContext;
import com.example.chatapp.infrastructure.auth.JwtTokenProvider;
import com.example.chatapp.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {MessageController.class},
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GlobalExceptionHandler.class))
@AutoConfigureMockMvc(addFilters = false)
public class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthContext authContext;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("채팅방 ID가 주어졌을 때, 메시지 목록 조회 시 페이징된 메시지가 반환되어야 함")
    void givenRoomId_whenGetRoomMessages_thenReturnPaginatedMessages() throws Exception {
        // Given
        Long roomId = 1L;
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);

        List<MessageResponse> messageList = List.of(
                createMessageResponse(3L, "Hello!", 1L, roomId),
                createMessageResponse(2L, "How are you?", 2L, roomId),
                createMessageResponse(1L, "I'm fine, thanks!", 1L, roomId)
        );

        Page<MessageResponse> messagePage = new PageImpl<>(messageList, pageable, messageList.size());
        when(messageService.findChatRoomMessages(roomId, pageable)).thenReturn(messagePage);

        // When & Then
        mockMvc.perform(get("/api/messages/room/{roomId}", roomId)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].id").value(3L))
                .andExpect(jsonPath("$.content[0].content").value("Hello!"))
                .andExpect(jsonPath("$.content[0].sender.id").value(1L))
                .andExpect(jsonPath("$.content[0].chatRoomId").value(roomId))
                .andExpect(jsonPath("$.content[0].status").value(MessageStatus.SENT.toString()))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(size));

    }

    @Test
    @DisplayName("채팅방 ID가 주어졌을 때, 최근 메시지 요청 시 메시지 목록이 반환되어야 함")
    void givenRoomId_whenGetRecentMessages_thenReturnMessagesList() throws Exception {
        // Given
        Long roomId = 1L;
        int limit = 50;

        List<MessageResponse> messages = List.of(
                createMessageResponse(3L, "Message 3", 2L, roomId),
                createMessageResponse(2L, "Message 2", 1L, roomId),
                createMessageResponse(1L, "Message 1", 2L, roomId)
        );

        when(messageService.findRecentChatRoomMessages(roomId, limit)).thenReturn(messages);

        // When & Then
        mockMvc.perform(get("/api/messages/room/{roomId}/recent", roomId)
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(3L))
                .andExpect(jsonPath("$[0].content").value("Message 3"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].content").value("Message 2"))
                .andExpect(jsonPath("$[2].id").value(1L))
                .andExpect(jsonPath("$[2].content").value("Message 1"));

        verify(messageService).findRecentChatRoomMessages(roomId, limit);
    }

    @Test
    @DisplayName("메시지 ID와 새 상태가 주어졌을 때, 상태 업데이트 요청 시 업데이트된 메시지가 반환되어야 함")
    void givenMessageIdAndNewStatus_whenUpdateStatus_thenReturnUpdatedMessage() throws Exception {
        // Given
        Long messageId = 1L;
        Long userId = 2L;
        MessageStatus newStatus = MessageStatus.READ;

        MessageStatusUpdateRequest request = new MessageStatusUpdateRequest();
        request.setUserId(userId);
        request.setStatus(newStatus);

        MessageResponse updatedMessage = createMessageResponse(messageId, "Test message", userId, 1L);
        updatedMessage.setStatus(newStatus);

        when(messageService.updateMessageStatus(messageId, userId, newStatus)).thenReturn(updatedMessage);

        // When & Then
        mockMvc.perform(patch("/api/messages/{id}/status", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(messageId))
                .andExpect(jsonPath("$.content").value("Test message"))
                .andExpect(jsonPath("$.sender.id").value(userId))
                .andExpect(jsonPath("$.status").value(newStatus.toString()));

        verify(messageService).updateMessageStatus(messageId, userId, newStatus);
    }

    // 테스트 도우미 메서드
    private MessageResponse createMessageResponse(Long id, String content, Long senderId, Long chatRoomId) {
        UserResponse sender = UserResponse.builder()
                .id(senderId)
                .username("user" + senderId)
                .build();

        return MessageResponse.builder()
                .id(id)
                .content(content)
                .sender(sender)
                .chatRoomId(chatRoomId)
                .status(MessageStatus.SENT)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
