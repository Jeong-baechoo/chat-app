package com.example.chatapp.service;

import com.example.chatapp.domain.*;
import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.dto.response.ChatRoomSimpleResponse;
import com.example.chatapp.dto.response.ParticipantResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.mapper.ChatRoomMapper;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.impl.ChatRoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
@MockitoSettings(strictness = Strictness.LENIENT) // 테스트 엄격도 완화
class ChatRoomServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Mock
    private ChatRoomMapper chatRoomMapper;

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User testUser;
    private User adminUser;
    private ChatRoom testChatRoom;
    private ChatRoomParticipant testParticipant;
    private ChatRoomParticipant adminParticipant;
    private ChatRoomResponse testChatRoomResponse;
    private ChatRoomSimpleResponse testChatRoomSimpleResponse;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        // Setup admin user
        adminUser = User.builder()
                .id(2L)
                .username("adminuser")
                .build();

        // Setup test chat room
        testChatRoom = new ChatRoom();
        testChatRoom.setId(1L);
        testChatRoom.setName("Test Room");
        testChatRoom.setType(ChatRoomType.GROUP);
        testChatRoom.setCreatedAt(LocalDateTime.now());
        List<ChatRoomParticipant> participants = new ArrayList<>();
        testChatRoom.setParticipants(participants);

        // Setup participant responses
        List<ParticipantResponse> participantResponses = new ArrayList<>();
        participantResponses.add(ParticipantResponse.builder()
                .userId(1L)
                .username("testuser")
                .role(ParticipantRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build());

        // Setup test participant
        testParticipant = ChatRoomParticipant.builder()
                .id(1L)
                .user(testUser)
                .chatRoom(testChatRoom)
                .role(ParticipantRole.MEMBER)
                .notificationEnabled(true)
                .joinedAt(LocalDateTime.now())
                .build();

        // Setup admin participant
        adminParticipant = ChatRoomParticipant.builder()
                .id(2L)
                .user(adminUser)
                .chatRoom(testChatRoom)
                .role(ParticipantRole.ADMIN)
                .notificationEnabled(true)
                .joinedAt(LocalDateTime.now())
                .build();

        testChatRoom.getParticipants().add(testParticipant);
        testChatRoom.getParticipants().add(adminParticipant);

        // Setup test chat room response
        testChatRoomResponse = ChatRoomResponse.builder()
                .id(1L)
                .name("Test Room")
                .type(ChatRoomType.GROUP)
                .participants(participantResponses)
                .createdAt(LocalDateTime.now())
                .build();

        // Setup test chat room simple response
        testChatRoomSimpleResponse = new ChatRoomSimpleResponse(1L, "Test Room", ChatRoomType.GROUP);

        // Setup SecurityContext mock
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Nested
    @DisplayName("채팅방 생성 테스트")
    class CreateChatRoomTests {

        @Test
        @DisplayName("채팅방 생성 성공")
        void createChatRoom_Success() {
            // Given
            ChatRoomCreateRequest createDTO = new ChatRoomCreateRequest();
            createDTO.setName("New Chat Room");
            createDTO.setType(ChatRoomType.GROUP);
            createDTO.setCreatorId(1L);

            // any() matcher 사용으로 인자 불일치 문제 해결
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomMapper.toEntity(any(ChatRoomCreateRequest.class))).thenReturn(testChatRoom);
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(testChatRoom);
            when(chatRoomMapper.toResponse(any(ChatRoom.class))).thenReturn(testChatRoomResponse);

            // When
            ChatRoomResponse result = chatRoomService.createChatRoom(createDTO);

            // Then
            assertNotNull(result);
            assertEquals(testChatRoomResponse.getId(), result.getId());
            assertEquals(testChatRoomResponse.getName(), result.getName());

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
            assertEquals(true, capturedParticipant.getNotificationEnabled());
        }

        @Test
        @DisplayName("채팅방 생성 실패 - 사용자 없음")
        void createChatRoom_UserNotFound() {
            // Given
            ChatRoomCreateRequest createDTO = new ChatRoomCreateRequest();
            createDTO.setName("New Chat Room");
            createDTO.setType(ChatRoomType.GROUP);
            createDTO.setCreatorId(999L);

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(UserException.class, () -> chatRoomService.createChatRoom(createDTO));
            verify(chatRoomRepository, never()).save(any());
            verify(chatRoomParticipantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("채팅방 조회 테스트")
    class FindChatRoomTests {

        @Test
        @DisplayName("모든 채팅방 조회 성공")
        void findAllChatRooms_Success() {
            // Given
            List<ChatRoom> chatRooms = Arrays.asList(testChatRoom);
            when(chatRoomRepository.findAllWithParticipants()).thenReturn(chatRooms);
            when(chatRoomMapper.toResponse(any(ChatRoom.class))).thenReturn(testChatRoomResponse);

            // When
            List<ChatRoomResponse> result = chatRoomService.findAllChatRooms();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testChatRoomResponse.getId(), result.getFirst().getId());
            assertEquals(testChatRoomResponse.getName(), result.getFirst().getName());
            verify(chatRoomRepository).findAllWithParticipants();
        }

        @Test
        @DisplayName("모든 채팅방 간단 정보 조회 성공")
        void findAllChatRoomsSimple_Success() {
            // Given
            List<ChatRoomSimpleResponse> simpleResponses = Arrays.asList(testChatRoomSimpleResponse);
            when(chatRoomRepository.findAllRoomsAsSimpleDto()).thenReturn(simpleResponses);

            // When
            List<ChatRoomSimpleResponse> result = chatRoomService.findAllChatRoomsSimple();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testChatRoomSimpleResponse.getId(), result.getFirst().getId());
            assertEquals(testChatRoomSimpleResponse.getName(), result.getFirst().getName());
            verify(chatRoomRepository).findAllRoomsAsSimpleDto();
        }

        @Test
        @DisplayName("ID로 채팅방 조회 성공")
        void findChatRoomById_Success() {
            // Given
            when(chatRoomRepository.findByIdWithParticipants(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatRoomMapper.toResponse(testChatRoom)).thenReturn(testChatRoomResponse);

            // When
            Optional<ChatRoomResponse> result = chatRoomService.findChatRoomById(1L);

            // Then
            assertTrue(result.isPresent());
            assertEquals(testChatRoomResponse.getId(), result.get().getId());
            assertEquals(testChatRoomResponse.getName(), result.get().getName());
            verify(chatRoomRepository).findByIdWithParticipants(1L);
        }

        @Test
        @DisplayName("ID로 채팅방 조회 실패 - 존재하지 않는 채팅방")
        void findChatRoomById_NotFound() {
            // Given
            when(chatRoomRepository.findByIdWithParticipants(999L)).thenReturn(Optional.empty());

            // When
            Optional<ChatRoomResponse> result = chatRoomService.findChatRoomById(999L);

            // Then
            assertFalse(result.isPresent());
            verify(chatRoomRepository).findByIdWithParticipants(999L);
        }

        @Test
        @DisplayName("사용자별 채팅방 목록 조회 성공")
        void findChatRoomsByUser_Success() {
            // Given
            List<ChatRoom> chatRooms = Arrays.asList(testChatRoom);
            when(userRepository.existsById(1L)).thenReturn(true);
            when(chatRoomRepository.findAllByParticipantUserId(1L)).thenReturn(chatRooms);
            when(chatRoomMapper.toResponse(any(ChatRoom.class))).thenReturn(testChatRoomResponse);

            // When
            List<ChatRoomResponse> result = chatRoomService.findChatRoomsByUser(1L);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testChatRoomResponse.getId(), result.getFirst().getId());
            assertEquals(testChatRoomResponse.getName(), result.getFirst().getName());
            verify(userRepository).existsById(1L);
            verify(chatRoomRepository).findAllByParticipantUserId(1L);
        }

        @Test
        @DisplayName("사용자별 채팅방 목록 조회 실패 - 사용자 없음")
        void findChatRoomsByUser_UserNotFound() {
            // Given
            when(userRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThrows(UserException.class, () -> chatRoomService.findChatRoomsByUser(999L));
            verify(userRepository).existsById(999L);
            verify(chatRoomRepository, never()).findAllByParticipantUserId(anyLong());
        }
    }

    @Nested
    @DisplayName("채팅방 참가자 관리 테스트")
    class ParticipantManagementTests {

        @Test
        @DisplayName("채팅방 참가자 추가 성공")
        void addParticipantToChatRoom_Success() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(userRepository.findById(3L)).thenReturn(Optional.of(new User()));
            when(chatRoomParticipantRepository.existsByUserIdAndChatRoomId(3L, 1L)).thenReturn(false);
            when(chatRoomMapper.toResponse(testChatRoom)).thenReturn(testChatRoomResponse);

            // When
            ChatRoomResponse result = chatRoomService.addParticipantToChatRoom(1L, 3L);

            // Then
            assertNotNull(result);
            assertEquals(testChatRoomResponse.getId(), result.getId());

            // Verify participant creation
            ArgumentCaptor<ChatRoomParticipant> participantCaptor = ArgumentCaptor.forClass(ChatRoomParticipant.class);
            verify(chatRoomParticipantRepository).save(participantCaptor.capture());
            ChatRoomParticipant capturedParticipant = participantCaptor.getValue();

            assertEquals(ParticipantRole.MEMBER, capturedParticipant.getRole());
            assertEquals(testChatRoom, capturedParticipant.getChatRoom());
            assertEquals(true, capturedParticipant.getNotificationEnabled());
        }

        @Test
        @DisplayName("채팅방 참가자 추가 실패 - 채팅방 없음")
        void addParticipantToChatRoom_ChatRoomNotFound() {
            // Given
            when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ChatRoomException.class, () -> chatRoomService.addParticipantToChatRoom(999L, 1L));
            verify(chatRoomRepository).findById(999L);
            verify(userRepository, never()).findById(anyLong());
            verify(chatRoomParticipantRepository, never()).save(any());
        }

        @Test
        @DisplayName("채팅방 참가자 추가 실패 - 사용자 없음")
        void addParticipantToChatRoom_UserNotFound() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(UserException.class, () -> chatRoomService.addParticipantToChatRoom(1L, 999L));
            verify(chatRoomRepository).findById(1L);
            verify(userRepository).findById(999L);
            verify(chatRoomParticipantRepository, never()).save(any());
        }

        @Test
        @DisplayName("채팅방 참가자 추가 - 이미 멤버인 경우")
        void addParticipantToChatRoom_AlreadyMember() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomParticipantRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);
            when(chatRoomMapper.toResponse(testChatRoom)).thenReturn(testChatRoomResponse);

            // When
            ChatRoomResponse result = chatRoomService.addParticipantToChatRoom(1L, 1L);

            // Then
            assertNotNull(result);
            assertEquals(testChatRoomResponse.getId(), result.getId());
            verify(chatRoomParticipantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("채팅방 삭제 테스트")
    class DeleteChatRoomTests {

        @Test
        @DisplayName("채팅방 삭제 성공 - 관리자 권한")
        void deleteChatRoom_Success() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(authentication.getName()).thenReturn("adminuser");
            when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(adminUser));
            when(chatRoomParticipantRepository.findByUserIdAndChatRoomId(2L, 1L))
                    .thenReturn(Optional.of(adminParticipant));

            // When
            chatRoomService.deleteChatRoom(1L);

            // Then
            verify(chatRoomRepository).deleteById(1L);
        }

        @Test
        @DisplayName("채팅방 삭제 실패 - 채팅방 없음")
        void deleteChatRoom_ChatRoomNotFound() {
            // Given
            when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ChatRoomException.class, () -> chatRoomService.deleteChatRoom(999L));
            verify(chatRoomRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("채팅방 삭제 실패 - 사용자 없음")
        void deleteChatRoom_UserNotFound() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(authentication.getName()).thenReturn("nonexistentuser");
            when(userRepository.findByUsername("nonexistentuser")).thenReturn(Optional.empty());

            // When & Then
            assertThrows(UserException.class, () -> chatRoomService.deleteChatRoom(1L));
            verify(chatRoomRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("채팅방 삭제 실패 - 권한 없음")
        void deleteChatRoom_NotAdmin() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(authentication.getName()).thenReturn("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(chatRoomParticipantRepository.findByUserIdAndChatRoomId(1L, 1L))
                    .thenReturn(Optional.of(testParticipant));

            // When & Then
            assertThrows(ChatRoomException.class, () -> chatRoomService.deleteChatRoom(1L));
            verify(chatRoomRepository, never()).deleteById(anyLong());
        }
    }
}
