package com.example.chatapp.controller;

import com.example.chatapp.domain.ChatRoomType;
import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.infrastructure.auth.AuthContext;
import com.example.chatapp.service.ChatRoomService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatRoomService chatRoomService;

    @MockBean
    private AuthContext authContext; // 이 부분을 추가

    @Test
    void createRoom_WithoutCreatorId_ShouldSetCreatorIdFromAttribute() throws Exception {
        // Given
        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName("테스트 채팅방");
        request.setType(ChatRoomType.GROUP);
        // creatorId는 의도적으로 설정하지 않음

        ChatRoomResponse mockResponse = new ChatRoomResponse();
        mockResponse.setId(1L);

        when(chatRoomService.createChatRoom(any())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request))
                        .requestAttr("userId", 123L)) // userId 속성 설정
                .andExpect(status().isCreated());

        // 서비스에 전달된 요청에 userId가 creatorId로 설정되었는지 확인
        ArgumentCaptor<ChatRoomCreateRequest> requestCaptor = ArgumentCaptor.forClass(ChatRoomCreateRequest.class);
        verify(chatRoomService).createChatRoom(requestCaptor.capture());
        assertEquals(123L, requestCaptor.getValue().getCreatorId());
    }

    @Test
    void createRoom_ShouldCreateRoomSuccessfully() throws Exception {
        // Given
        Long userId = 123L;
        when(authContext.getCurrentUserId()).thenReturn(userId); // AuthContext 동작 설정

        ChatRoomCreateRequest request = new ChatRoomCreateRequest();
        request.setName("테스트 채팅방");
        request.setType(ChatRoomType.GROUP);

        ChatRoomResponse mockResponse = new ChatRoomResponse();
        mockResponse.setId(1L);
        when(chatRoomService.createChatRoom(any())).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/chatrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 서비스에 전달된 요청에 userId가 creatorId로 설정되었는지 확인
        ArgumentCaptor<ChatRoomCreateRequest> requestCaptor = ArgumentCaptor.forClass(ChatRoomCreateRequest.class);
        verify(chatRoomService).createChatRoom(requestCaptor.capture());
        assertEquals(userId, requestCaptor.getValue().getCreatorId());
    }
}
