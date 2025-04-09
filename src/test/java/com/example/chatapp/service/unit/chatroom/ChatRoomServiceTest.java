package com.example.chatapp.service.unit.chatroom;

import com.example.chatapp.domain.*;
import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long CHAT_ROOM_ID = 1L;
    private static final Long NONEXISTENT_ID = 999L;
    private static final String TEST_USERNAME = "testuser";
    private static final String ADMIN_USERNAME = "adminuser";
    private static final String TEST_ROOM_NAME = "Test Room";
    private static final String NEW_ROOM_NAME = "New Chat Room";
    private static final String ERROR_CHAT_ROOM_NOT_FOUND = "채팅방을 찾을 수 없습니다";
    private static final String ERROR_USER_NOT_FOUND = "사용자를 찾을 수 없습니다";
    private static final String ERROR_NOT_ADMIN = "채팅방 관리자만 삭제할 수 있습니다";

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Mock
    private ChatRoomMapper chatRoomMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private ChatRoomServiceImpl chatRoomService;

    private User testUser;
    private User adminUser;
    private ChatRoom testChatRoom;
    private ChatRoomParticipant testParticipant;
    private ChatRoomParticipant adminParticipant;
    private ChatRoomResponse testChatRoomResponse;

    @BeforeEach
    void setUp() {
        chatRoomService = new ChatRoomServiceImpl(
                chatRoomRepository,
                userRepository,
                chatRoomParticipantRepository,
                chatRoomMapper
        );

        // 테스트 사용자 설정
        testUser = User.builder()
                .id(USER_ID)
                .username(TEST_USERNAME)
                .build();

        // 관리자 사용자 설정
        adminUser = User.builder()
                .id(ADMIN_ID)
                .username(ADMIN_USERNAME)
                .build();

        // 테스트 채팅방 설정
        testChatRoom = new ChatRoom();
        testChatRoom.setId(CHAT_ROOM_ID);
        testChatRoom.setName(TEST_ROOM_NAME);
        testChatRoom.setType(ChatRoomType.GROUP);
        testChatRoom.setCreatedAt(LocalDateTime.now());
        List<ChatRoomParticipant> participants = new ArrayList<>();
        testChatRoom.setParticipants(participants);

        // 참가자 응답 설정
        List<ParticipantResponse> participantResponses = new ArrayList<>();
        participantResponses.add(ParticipantResponse.builder()
                .userId(USER_ID)
                .username(TEST_USERNAME)
                .role(ParticipantRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build());

        // 일반 참가자 설정
        testParticipant = ChatRoomParticipant.builder()
                .id(1L)
                .user(testUser)
                .chatRoom(testChatRoom)
                .role(ParticipantRole.MEMBER)
                .notificationEnabled(true)
                .joinedAt(LocalDateTime.now())
                .build();

        // 관리자 참가자 설정
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

        // 채팅방 응답 객체 설정
        testChatRoomResponse = ChatRoomResponse.builder()
                .id(CHAT_ROOM_ID)
                .name(TEST_ROOM_NAME)
                .type(ChatRoomType.GROUP)
                .participants(participantResponses)
                .createdAt(LocalDateTime.now())
                .build();

        // SecurityContext 목 설정
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Nested
    @DisplayName("채팅방 생성 테스트")
    class CreateChatRoomTests {

        @Test
        @DisplayName("givenValidRequest_whenCreateChatRoom_thenChatRoomCreated")
        void givenValidRequest_whenCreateChatRoom_thenChatRoomCreated() {
            // Given
            ChatRoomCreateRequest createDTO = new ChatRoomCreateRequest();
            createDTO.setName(NEW_ROOM_NAME);
            createDTO.setType(ChatRoomType.GROUP);
            createDTO.setCreatorId(USER_ID);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(chatRoomMapper.toEntity(any(ChatRoomCreateRequest.class))).thenReturn(testChatRoom);
            when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(testChatRoom);
            when(chatRoomMapper.toResponse(any(ChatRoom.class))).thenReturn(testChatRoomResponse);

            // When
            ChatRoomResponse result = chatRoomService.createChatRoom(createDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(CHAT_ROOM_ID);
            assertThat(result.getName()).isEqualTo(TEST_ROOM_NAME);

            // 사용자 조회 및 채팅방 저장 검증
            verify(userRepository).findById(USER_ID);
            verify(chatRoomRepository).save(any(ChatRoom.class));
            
            // 참가자 생성 검증
            ArgumentCaptor<ChatRoomParticipant> participantCaptor = ArgumentCaptor.forClass(ChatRoomParticipant.class);
            verify(chatRoomParticipantRepository).save(participantCaptor.capture());
            ChatRoomParticipant capturedParticipant = participantCaptor.getValue();

            assertThat(capturedParticipant.getRole()).isEqualTo(ParticipantRole.ADMIN);
            assertThat(capturedParticipant.getUser()).isEqualTo(testUser);
            assertThat(capturedParticipant.getChatRoom()).isEqualTo(testChatRoom);
            assertThat(capturedParticipant.getNotificationEnabled()).isTrue();
        }

        @Test
        @DisplayName("givenNonExistentUser_whenCreateChatRoom_thenThrowUserException")
        void givenNonExistentUser_whenCreateChatRoom_thenThrowUserException() {
            // Given
            ChatRoomCreateRequest createDTO = new ChatRoomCreateRequest();
            createDTO.setName(NEW_ROOM_NAME);
            createDTO.setType(ChatRoomType.GROUP);
            createDTO.setCreatorId(NONEXISTENT_ID);

            when(userRepository.findById(NONEXISTENT_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatRoomService.createChatRoom(createDTO))
                    .isInstanceOf(UserException.class);
            
            verify(chatRoomRepository, never()).save(any());
            verify(chatRoomParticipantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("채팅방 참가자 관리 테스트")
    class ParticipantManagementTests {

        @Test
        @DisplayName("givenChatRoomAndUserId_whenAddParticipant_thenParticipantAdded")
        void givenChatRoomAndUserId_whenAddParticipant_thenParticipantAdded() {
            // Given
            Long newUserId = 3L;
            User newUser = User.builder().id(newUserId).username("newuser").build();
            
            when(chatRoomRepository.findById(CHAT_ROOM_ID)).thenReturn(Optional.of(testChatRoom));
            when(userRepository.findById(newUserId)).thenReturn(Optional.of(newUser));
            when(chatRoomParticipantRepository.existsByUserIdAndChatRoomId(newUserId, CHAT_ROOM_ID)).thenReturn(false);
            when(chatRoomMapper.toResponse(testChatRoom)).thenReturn(testChatRoomResponse);

            // When
            ChatRoomResponse result = chatRoomService.addParticipantToChatRoom(CHAT_ROOM_ID, newUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(CHAT_ROOM_ID);

            // 참가자 생성 검증
            ArgumentCaptor<ChatRoomParticipant> participantCaptor = ArgumentCaptor.forClass(ChatRoomParticipant.class);
            verify(chatRoomParticipantRepository).save(participantCaptor.capture());
            ChatRoomParticipant capturedParticipant = participantCaptor.getValue();

            assertThat(capturedParticipant.getRole()).isEqualTo(ParticipantRole.MEMBER);
            assertThat(capturedParticipant.getChatRoom()).isEqualTo(testChatRoom);
            assertThat(capturedParticipant.getUser()).isEqualTo(newUser);
            assertThat(capturedParticipant.getNotificationEnabled()).isTrue();
        }

        @Test
        @DisplayName("givenExistingParticipant_whenAddParticipant_thenNoNewParticipantAdded")
        void givenExistingParticipant_whenAddParticipant_thenNoNewParticipantAdded() {
            // Given
            when(chatRoomRepository.findById(CHAT_ROOM_ID)).thenReturn(Optional.of(testChatRoom));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(chatRoomParticipantRepository.existsByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID)).thenReturn(true);
            when(chatRoomMapper.toResponse(testChatRoom)).thenReturn(testChatRoomResponse);

            // When
            ChatRoomResponse result = chatRoomService.addParticipantToChatRoom(CHAT_ROOM_ID, USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(CHAT_ROOM_ID);
            
            // 이미 멤버이므로 참가자 저장이 호출되지 않음
            verify(chatRoomParticipantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("채팅방 삭제 테스트")
    class DeleteChatRoomTests {

        @Test
        @DisplayName("givenAdminUser_whenDeleteChatRoom_thenChatRoomDeleted")
        void givenAdminUser_whenDeleteChatRoom_thenChatRoomDeleted() {
            // Given
            when(chatRoomRepository.findById(CHAT_ROOM_ID)).thenReturn(Optional.of(testChatRoom));
            when(authentication.getName()).thenReturn(ADMIN_USERNAME);
            when(userRepository.findByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(adminUser));
            when(chatRoomParticipantRepository.findByUserIdAndChatRoomId(ADMIN_ID, CHAT_ROOM_ID))
                    .thenReturn(Optional.of(adminParticipant));

            // When
            chatRoomService.deleteChatRoom(CHAT_ROOM_ID);

            // Then
            verify(chatRoomRepository).deleteById(CHAT_ROOM_ID);
        }

        @Test
        @DisplayName("givenNonExistentChatRoom_whenDeleteChatRoom_thenThrowChatRoomException")
        void givenNonExistentChatRoom_whenDeleteChatRoom_thenThrowChatRoomException() {
            // Given
            when(chatRoomRepository.findById(NONEXISTENT_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatRoomService.deleteChatRoom(NONEXISTENT_ID))
                    .isInstanceOf(ChatRoomException.class);
                    
            verify(chatRoomRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("givenNonAdminUser_whenDeleteChatRoom_thenThrowChatRoomException")
        void givenNonAdminUser_whenDeleteChatRoom_thenThrowChatRoomException() {
            // Given
            when(chatRoomRepository.findById(CHAT_ROOM_ID)).thenReturn(Optional.of(testChatRoom));
            when(authentication.getName()).thenReturn(TEST_USERNAME);
            when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
            when(chatRoomParticipantRepository.findByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID))
                    .thenReturn(Optional.of(testParticipant));

            // When & Then
            assertThatThrownBy(() -> chatRoomService.deleteChatRoom(CHAT_ROOM_ID))
                    .isInstanceOf(ChatRoomException.class);
                    
            verify(chatRoomRepository, never()).deleteById(anyLong());
        }
    }
}
