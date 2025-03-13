package com.example.chatapp.service;

import com.example.chatapp.domain.*;
import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.impl.ChatRoomServiceImpl;
import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @InjectMocks
    private ChatRoomServiceImpl chatRoomServiceImpl;

    private User testUser;
    private ChatRoom testChatRoom;
    private ChatRoomParticipant testParticipant;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // Setup test chat room
        testChatRoom = new ChatRoom();
        testChatRoom.setId(1L);
        testChatRoom.setName("Test Room");
        testChatRoom.setType(ChatRoomType.GROUP);
        testChatRoom.setParticipants(new ArrayList<>());

        // Setup test participant
        testParticipant = ChatRoomParticipant.builder()
                .id(1L)
                .user(testUser)
                .chatRoom(testChatRoom)
                .role(ParticipantRole.ADMIN)
                .notificationEnabled(true)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("채팅방 생성 성공")
    void createChatRoom_Success() {
        // Given
        @Valid ChatRoomCreateRequest createDTO = new ChatRoomCreateRequest();
        createDTO.setName("New Chat Room");
        createDTO.setType(ChatRoomType.GROUP);
        createDTO.setCreatorId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(testChatRoom);

        // When
        ChatRoomResponse result = chatRoomServiceImpl.createChatRoom(createDTO);

        // Then
        assertNotNull(result);
        assertEquals("Test Room", result.getName());

        // Verify repository calls
        verify(userRepository).findById(1L);
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomParticipantRepository).save(any(ChatRoomParticipant.class));

        // Verify participant creation
        ArgumentCaptor<ChatRoomParticipant> participantCaptor = ArgumentCaptor.forClass(ChatRoomParticipant.class);
        verify(chatRoomParticipantRepository).save(participantCaptor.capture());
        ChatRoomParticipant capturedParticipant = participantCaptor.getValue();

        assertEquals(ParticipantRole.ADMIN, capturedParticipant.getRole());
        assertEquals(testUser, capturedParticipant.getUser());
        assertEquals(testChatRoom, capturedParticipant.getChatRoom());
    }

    @Test
    @DisplayName("채팅방 생성 실패 - 사용자 없음")
    void createChatRoom_UserNotFound() {
        // Given
        @Valid ChatRoomCreateRequest createDTO = new ChatRoomCreateRequest();
        createDTO.setName("New Chat Room");
        createDTO.setType(ChatRoomType.GROUP);
        createDTO.setCreatorId(999L);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserException.class, () -> chatRoomServiceImpl.createChatRoom(createDTO));
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("모든 채팅방 조회 성공")
    void findAllChatRooms_Success() {
        // Given
        List<ChatRoom> chatRooms = Arrays.asList(testChatRoom);
        when(chatRoomRepository.findAll()).thenReturn(chatRooms);

        // When
        List<ChatRoomResponse> result = chatRoomServiceImpl.findAllChatRooms();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testChatRoom.getId(), result.get(0).getId());
        assertEquals(testChatRoom.getName(), result.get(0).getName());
    }

    @Test
    @DisplayName("ID로 채팅방 조회 성공")
    void findChatRoomById_Success() {
        // Given
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));

        // When
        Optional<ChatRoomResponse> result = chatRoomServiceImpl.findChatRoomById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testChatRoom.getId(), result.get().getId());
        assertEquals(testChatRoom.getName(), result.get().getName());
    }

    @Test
    @DisplayName("ID로 채팅방 조회 실패 - 존재하지 않는 채팅방")
    void findChatRoomById_NotFound() {
        // Given
        when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<ChatRoomResponse> result = chatRoomServiceImpl.findChatRoomById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("사용자별 채팅방 목록 조회 성공")
    void findChatRoomsByUser_Success() {
        // Given
        List<ChatRoomParticipant> participants = Arrays.asList(testParticipant);
        when(chatRoomParticipantRepository.findByUserId(1L)).thenReturn(participants);

        // When
        List<ChatRoomResponse> result = chatRoomServiceImpl.findChatRoomsByUser(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testChatRoom.getId(), result.get(0).getId());
    }

    @Test
    @DisplayName("채팅방 참가자 추가 성공")
    void addParticipantToChatRoom_Success() {
        // Given
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(userRepository.findById(2L)).thenReturn(Optional.of(new User()));
        when(chatRoomParticipantRepository.existsByUserIdAndChatRoomId(2L, 1L)).thenReturn(false);

        // When
        ChatRoomResponse result = chatRoomServiceImpl.addParticipantToChatRoom(1L, 2L);

        // Then
        assertNotNull(result);
        verify(chatRoomParticipantRepository).save(any(ChatRoomParticipant.class));

        // Verify participant creation
        ArgumentCaptor<ChatRoomParticipant> participantCaptor = ArgumentCaptor.forClass(ChatRoomParticipant.class);
        verify(chatRoomParticipantRepository).save(participantCaptor.capture());
        ChatRoomParticipant capturedParticipant = participantCaptor.getValue();

        assertEquals(ParticipantRole.MEMBER, capturedParticipant.getRole());
    }

    @Test
    @DisplayName("채팅방 참가자 추가 - 이미 멤버인 경우")
    void addParticipantToChatRoom_AlreadyMember() {
        // Given
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomParticipantRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);

        // When
        ChatRoomResponse result = chatRoomServiceImpl.addParticipantToChatRoom(1L, 1L);

        // Then
        assertNotNull(result);
        verify(chatRoomParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅방 삭제 성공")
    void deleteChatRoom_Success() {
        // Given
        when(chatRoomRepository.existsById(1L)).thenReturn(true);

        // When
        chatRoomServiceImpl.deleteChatRoom(1L);

        // Then
        verify(chatRoomRepository).deleteById(1L);
    }

    @Test
    @DisplayName("채팅방 삭제 실패 - 존재하지 않는 채팅방")
    void deleteChatRoom_NotFound() {
        // Given
        when(chatRoomRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> chatRoomServiceImpl.deleteChatRoom(999L));
        verify(chatRoomRepository, never()).deleteById(anyLong());
    }
}
