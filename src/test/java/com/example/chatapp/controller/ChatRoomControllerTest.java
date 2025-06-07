package com.example.chatapp.controller;

import com.example.chatapp.domain.ChatRoomType;
import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.request.ChatRoomJoinRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.dto.response.ChatRoomSimpleResponse;
import com.example.chatapp.dto.response.ParticipantResponse;
import com.example.chatapp.config.WebFilterConfig;
import com.example.chatapp.exception.GlobalExceptionHandler;
import com.example.chatapp.infrastructure.auth.AuthContext;
import com.example.chatapp.infrastructure.auth.JwtTokenProvider;
import com.example.chatapp.infrastructure.filter.SessionAuthenticationFilter;
import com.example.chatapp.service.ChatRoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ChatRoomController.class}, 
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GlobalExceptionHandler.class))
@AutoConfigureMockMvc(addFilters = false)
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatRoomService chatRoomService;

    @MockitoBean
    private AuthContext authContext;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("전체 채팅방 조회 성공")
    void givenChatRoomsExist_whenGetAllRooms_thenReturnAllChatRooms() throws Exception {
        // Given
        List<ChatRoomSimpleResponse> chatRoomSimpleResponses = List.of(
                new ChatRoomSimpleResponse(1L, "testRoom1", ChatRoomType.GROUP),
                new ChatRoomSimpleResponse(2L, "testRoom2", ChatRoomType.PRIVATE)
        );
        when(chatRoomService.findAllChatRoomsSimple()).thenReturn(chatRoomSimpleResponses);

        // When
        ResultActions perform = mockMvc.perform(get("/api/rooms"));

        // Then
        perform.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("testRoom1"))
                .andExpect(jsonPath("$[0].type").value(ChatRoomType.GROUP.toString()))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("testRoom2"))
                .andExpect(jsonPath("$[1].type").value(ChatRoomType.PRIVATE.toString()));
    }

    @Test
    @DisplayName("특정 사용자의 채팅방 조회 성공")
    void givenUserWithChatRooms_whenGetMyRooms_thenReturnUserChatRooms() throws Exception {
        // Given
        Long userId = 1L;
        List<ParticipantResponse> participants = new ArrayList<>();
        participants.add(ParticipantResponse.builder()
                .userId(userId)
                .username("testuser")
                .joinedAt(LocalDateTime.now())
                .build());

        List<ChatRoomResponse> chatRooms = List.of(
                ChatRoomResponse.builder()
                        .id(1L)
                        .name("My Room 1")
                        .type(ChatRoomType.PRIVATE)
                        .participants(participants)
                        .createdAt(LocalDateTime.now())
                        .build(),
                ChatRoomResponse.builder()
                        .id(3L)
                        .name("My Room 2")
                        .type(ChatRoomType.GROUP)
                        .participants(participants)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(authContext.getCurrentUserId()).thenReturn(userId);
        when(chatRoomService.findChatRoomsByUser(userId)).thenReturn(chatRooms);

        // When & Then
        mockMvc.perform(get("/api/rooms/me"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("My Room 1"))
                .andExpect(jsonPath("$[0].type").value(ChatRoomType.PRIVATE.toString()))
                .andExpect(jsonPath("$[1].id").value(3L))
                .andExpect(jsonPath("$[1].name").value("My Room 2"))
                .andExpect(jsonPath("$[1].type").value(ChatRoomType.GROUP.toString()));
    }

    @Test
    @DisplayName("채팅방 상세 조회 성공")
    void givenValidRoomId_whenGetRoomById_thenReturnChatRoomDetails() throws Exception {
        // Given
        Long roomId = 1L;
        List<ParticipantResponse> participants = new ArrayList<>();
        participants.add(ParticipantResponse.builder()
                .userId(1L)
                .username("testuser")
                .joinedAt(LocalDateTime.now())
                .build());

        ChatRoomResponse response = ChatRoomResponse.builder()
                .id(roomId)
                .name("Test Room")
                .type(ChatRoomType.GROUP)
                .participants(participants)
                .createdAt(LocalDateTime.now())
                .build();

        when(chatRoomService.findChatRoomById(roomId)).thenReturn(Optional.of(response));

        // When & Then
        mockMvc.perform(get("/api/rooms/{id}", roomId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(roomId))
                .andExpect(jsonPath("$.name").value("Test Room"))
                .andExpect(jsonPath("$.type").value(ChatRoomType.GROUP.toString()))
                .andExpect(jsonPath("$.participants", hasSize(1)))
                .andExpect(jsonPath("$.participants[0].userId").value(1L));
    }

    @Test
    @DisplayName("존재하지 않는 채팅방 조회 실패")
    void givenInvalidRoomId_whenGetRoomById_thenReturnNotFound() throws Exception {
        // Given
        Long roomId = 999L;
        when(chatRoomService.findChatRoomById(roomId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/rooms/{id}", roomId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("채팅방 생성 성공")
    void testCreateRoom() throws Exception {
        // Given
        Long userId = 1L;
        ChatRoomCreateRequest request = ChatRoomCreateRequest.builder()
                .name("New Room")
                .type(ChatRoomType.GROUP)
                .build();

        ChatRoomResponse response = ChatRoomResponse.builder()
                .id(5L)
                .name("New Room")
                .type(ChatRoomType.GROUP)
                .participants(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();

        when(authContext.getCurrentUserId()).thenReturn(userId);
        when(chatRoomService.createChatRoom(any(ChatRoomCreateRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.name").value("New Room"))
                .andExpect(jsonPath("$.type").value(ChatRoomType.GROUP.toString()))
                .andExpect(header().string("Location", containsString("/api/rooms/5")));

        // 사용자 ID가 creatorId로 설정되었는지 확인
        verify(chatRoomService).createChatRoom(argThat(req ->
            req.getCreatorId().equals(userId) && req.getName().equals("New Room")
        ));
    }

    @Test
    @DisplayName("채팅방 참여 성공")
    void testJoinRoom() throws Exception {
        // Given
        Long roomId = 1L;
        Long userId = 2L;
        ChatRoomJoinRequest request = ChatRoomJoinRequest.builder()
                .userId(userId)
                .build();

        List<ParticipantResponse> participants = new ArrayList<>();
        participants.add(ParticipantResponse.builder()
                .userId(1L)
                .username("admin")
                .joinedAt(LocalDateTime.now().minusDays(1))
                .build());
        participants.add(ParticipantResponse.builder()
                .userId(userId)
                .username("newuser")
                .joinedAt(LocalDateTime.now())
                .build());

        ChatRoomResponse response = ChatRoomResponse.builder()
                .id(roomId)
                .name("Test Room")
                .type(ChatRoomType.GROUP)
                .participants(participants)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(authContext.getCurrentUserId()).thenReturn(userId);
        when(chatRoomService.addParticipantToChatRoom(roomId, userId)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/rooms/{id}/join", roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(roomId))
                .andExpect(jsonPath("$.participants", hasSize(2)))
                .andExpect(jsonPath("$.participants[1].userId").value(userId));

        verify(chatRoomService).addParticipantToChatRoom(roomId, userId);
    }

    @Test
    @DisplayName("채팅방 삭제 성공")
    void testDeleteRoom() throws Exception {
        // Given
        Long roomId = 1L;
        Long userId = 1L;
        when(authContext.getCurrentUserId()).thenReturn(userId);
        doNothing().when(chatRoomService).deleteChatRoom(roomId, userId);

        // When & Then
        mockMvc.perform(delete("/api/rooms/{id}", roomId))
                .andExpect(status().isNoContent());

        verify(authContext).getCurrentUserId();
        verify(chatRoomService).deleteChatRoom(roomId, userId);
    }
}
