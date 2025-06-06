package com.example.chatapp.service.unit.chatroom;

import com.example.chatapp.domain.*;
import com.example.chatapp.domain.service.ChatRoomDomainService;
import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.dto.response.ParticipantResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.infrastructure.event.ChatEventPublisherService;
import com.example.chatapp.mapper.ChatRoomMapper;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.service.EntityFinderService;
import com.example.chatapp.service.impl.ChatRoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    private static final String ERROR_NO_DELETE_PERMISSION = "채팅방을 삭제할 권한이 없습니다.";

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomParticipantRepository participantRepo;

    @Mock
    private ChatRoomMapper chatRoomMapper;

    @Mock
    private ChatEventPublisherService eventPublisher;

    @Mock
    private EntityFinderService entityFinderService;

    @Mock
    private ChatRoomDomainService chatRoomDomainService;

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    private User testUser;
    private User adminUser;
    private ChatRoom testChatRoom;
    private ChatRoomParticipant testParticipant;
    private ChatRoomParticipant adminParticipant;
    private ChatRoomResponse testChatRoomResponse;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 설정
        testUser = createTestUser(USER_ID, TEST_USERNAME);
        adminUser = createTestUser(ADMIN_ID, ADMIN_USERNAME);

        // 테스트 채팅방 설정
        testChatRoom = createTestChatRoom(CHAT_ROOM_ID, TEST_ROOM_NAME);

        // 참가자 응답 설정
        List<ParticipantResponse> participantResponses = new ArrayList<>();
        participantResponses.add(ParticipantResponse.builder()
                .userId(USER_ID)
                .username(TEST_USERNAME)
                .role(ParticipantRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build());

        // 일반 참가자 설정
        testParticipant = createTestParticipant(1L, testUser, testChatRoom, ParticipantRole.MEMBER);

        // 관리자 참가자 설정
        adminParticipant = createTestParticipant(2L, adminUser, testChatRoom, ParticipantRole.ADMIN);

        // 채팅방 응답 객체 설정
        testChatRoomResponse = ChatRoomResponse.builder()
                .id(CHAT_ROOM_ID)
                .name(TEST_ROOM_NAME)
                .type(ChatRoomType.GROUP)
                .participants(participantResponses)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // 테스트용 헬퍼 메서드들
    private User createTestUser(Long id, String username) {
        User user = User.create(username, "encoded_password");
        setField(user, "id", id);
        return user;
    }

    private ChatRoom createTestChatRoom(Long id, String name) {
        User tempUser = User.create("temp", "encoded_password");
        ChatRoom chatRoom = ChatRoom.create(name, ChatRoomType.GROUP, tempUser);
        setField(chatRoom, "id", id);
        return chatRoom;
    }

    private ChatRoomParticipant createTestParticipant(Long id, User user, ChatRoom chatRoom, ParticipantRole role) {
        ChatRoomParticipant participant = ChatRoomParticipant.of(user, chatRoom, role);
        setField(participant, "id", id);
        return participant;
    }

    // 리플렉션을 사용해 private 필드에 값 설정 (테스트용)
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    @Nested
    @DisplayName("채팅방 생성 테스트")
    class CreateChatRoomTests {

        @Test
        @DisplayName("유효한 요청으로 채팅방 생성 시 채팅방이 생성되어야 함")
        void givenValidRequest_whenCreateChatRoom_thenChatRoomCreated() {
            // Given
            ChatRoomCreateRequest createDTO = new ChatRoomCreateRequest();
            createDTO.setName(NEW_ROOM_NAME);
            createDTO.setType(ChatRoomType.GROUP);
            createDTO.setCreatorId(USER_ID);

            when(entityFinderService.findUserById(USER_ID)).thenReturn(testUser);
            when(chatRoomDomainService.createChatRoom(NEW_ROOM_NAME, ChatRoomType.GROUP, testUser))
                    .thenReturn(testChatRoom);
            when(chatRoomRepository.save(testChatRoom)).thenReturn(testChatRoom);
            when(chatRoomMapper.toResponse(testChatRoom)).thenReturn(testChatRoomResponse);

            // When
            ChatRoomResponse result = chatRoomService.createChatRoom(createDTO);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(CHAT_ROOM_ID);
            assertThat(result.getName()).isEqualTo(TEST_ROOM_NAME);

            // 검증
            verify(entityFinderService).findUserById(USER_ID);
            verify(chatRoomDomainService).createChatRoom(NEW_ROOM_NAME, ChatRoomType.GROUP, testUser);
            verify(chatRoomRepository).save(testChatRoom);
            verify(eventPublisher).publishRoomCreatedEvent(testChatRoom, testUser);
            verify(chatRoomMapper).toResponse(testChatRoom);
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 채팅방 생성 시 예외가 발생해야 함")
        void givenNonExistentUser_whenCreateChatRoom_thenThrowUserException() {
            // Given
            ChatRoomCreateRequest createDTO = new ChatRoomCreateRequest();
            createDTO.setName(NEW_ROOM_NAME);
            createDTO.setType(ChatRoomType.GROUP);
            createDTO.setCreatorId(NONEXISTENT_ID);

            when(entityFinderService.findUserById(NONEXISTENT_ID))
                    .thenThrow(new UserException(ERROR_USER_NOT_FOUND));

            // When & Then
            assertThatThrownBy(() -> chatRoomService.createChatRoom(createDTO))
                    .isInstanceOf(UserException.class)
                    .hasMessage(ERROR_USER_NOT_FOUND);

            verify(entityFinderService).findUserById(NONEXISTENT_ID);
            verify(chatRoomDomainService, never()).createChatRoom(any(), any(), any());
            verify(chatRoomRepository, never()).save(any());
            verify(eventPublisher, never()).publishRoomCreatedEvent(any(), any());
        }
    }

    @Nested
    @DisplayName("채팅방 참가자 관리 테스트")
    class ParticipantManagementTests {

        @Test
        @DisplayName("새로운 사용자를 채팅방에 추가할 때 참가자가 추가되어야 함")
        void givenChatRoomAndUserId_whenAddParticipant_thenParticipantAdded() {
            // Given
            Long newUserId = 3L;
            User newUser = createTestUser(newUserId, "newuser");

            when(chatRoomRepository.findById(CHAT_ROOM_ID)).thenReturn(Optional.of(testChatRoom));
            when(entityFinderService.findUserById(newUserId)).thenReturn(newUser);
            when(participantRepo.existsByUserIdAndChatRoomId(newUserId, CHAT_ROOM_ID))
                    .thenReturn(false);
            when(chatRoomMapper.toResponse(testChatRoom)).thenReturn(testChatRoomResponse);

            // When
            ChatRoomResponse result = chatRoomService.addParticipantToChatRoom(CHAT_ROOM_ID, newUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(CHAT_ROOM_ID);

            // 검증
            verify(chatRoomRepository).findById(CHAT_ROOM_ID);
            verify(entityFinderService).findUserById(newUserId);
            verify(participantRepo).existsByUserIdAndChatRoomId(newUserId, CHAT_ROOM_ID);
            verify(chatRoomDomainService).joinChatRoom(testChatRoom, newUser);
            verify(eventPublisher).publishUserJoinEvent(CHAT_ROOM_ID, newUser);
            verify(chatRoomMapper).toResponse(testChatRoom);
        }

        @Test
        @DisplayName("이미 참여한 사용자를 추가할 때 중복 추가되지 않아야 함")
        void givenExistingParticipant_whenAddParticipant_thenNoNewParticipantAdded() {
            // Given
            when(chatRoomRepository.findById(CHAT_ROOM_ID)).thenReturn(Optional.of(testChatRoom));
            when(entityFinderService.findUserById(USER_ID)).thenReturn(testUser);
            when(participantRepo.existsByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID))
                    .thenReturn(true);
            when(chatRoomMapper.toResponse(testChatRoom)).thenReturn(testChatRoomResponse);

            // When
            ChatRoomResponse result = chatRoomService.addParticipantToChatRoom(CHAT_ROOM_ID, USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(CHAT_ROOM_ID);

            // 검증 - 이미 멤버이므로 도메인 서비스나 이벤트 발행이 호출되지 않음
            verify(chatRoomDomainService, never()).joinChatRoom(any(), any());
            verify(eventPublisher, never()).publishUserJoinEvent(any(), any());
        }

        @Test
        @DisplayName("존재하지 않는 채팅방에 참가자 추가 시 예외가 발생해야 함")
        void givenNonExistentChatRoom_whenAddParticipant_thenThrowChatRoomException() {
            // Given
            when(chatRoomRepository.findById(NONEXISTENT_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatRoomService.addParticipantToChatRoom(NONEXISTENT_ID, USER_ID))
                    .isInstanceOf(ChatRoomException.class)
                    .hasMessage(ERROR_CHAT_ROOM_NOT_FOUND);

            verify(chatRoomRepository).findById(NONEXISTENT_ID);
            verify(chatRoomDomainService, never()).joinChatRoom(any(), any());
        }
    }

    @Nested
    @DisplayName("채팅방 삭제 테스트")
    class DeleteChatRoomTests {

        @Test
        @DisplayName("관리자 사용자가 채팅방 삭제 시 채팅방이 삭제되어야 함")
        void givenAdminUser_whenDeleteChatRoom_thenChatRoomDeleted() {
            // Given
            when(chatRoomRepository.findById(CHAT_ROOM_ID)).thenReturn(Optional.of(testChatRoom));
            when(participantRepo.findByUserIdAndChatRoomId(ADMIN_ID, CHAT_ROOM_ID))
                    .thenReturn(Optional.of(adminParticipant));

            // When
            chatRoomService.deleteChatRoom(CHAT_ROOM_ID, ADMIN_ID);

            // Then
            verify(chatRoomRepository).findById(CHAT_ROOM_ID);
            verify(participantRepo).findByUserIdAndChatRoomId(ADMIN_ID, CHAT_ROOM_ID);
            verify(chatRoomRepository).delete(testChatRoom);
        }

        @Test
        @DisplayName("존재하지 않는 채팅방 삭제 시 예외가 발생해야 함")
        void givenNonExistentChatRoom_whenDeleteChatRoom_thenThrowChatRoomException() {
            // Given
            when(chatRoomRepository.findById(NONEXISTENT_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatRoomService.deleteChatRoom(NONEXISTENT_ID, ADMIN_ID))
                    .isInstanceOf(ChatRoomException.class)
                    .hasMessage(ERROR_CHAT_ROOM_NOT_FOUND);

            verify(chatRoomRepository).findById(NONEXISTENT_ID);
            verify(chatRoomRepository, never()).delete(any());
        }

        @Test
        @DisplayName("일반 사용자가 채팅방 삭제 시 권한 예외가 발생해야 함")
        void givenNonAdminUser_whenDeleteChatRoom_thenThrowChatRoomException() {
            // Given
            when(chatRoomRepository.findById(CHAT_ROOM_ID)).thenReturn(Optional.of(testChatRoom));
            when(participantRepo.findByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID))
                    .thenReturn(Optional.of(testParticipant));

            // When & Then
            assertThatThrownBy(() -> chatRoomService.deleteChatRoom(CHAT_ROOM_ID, USER_ID))
                    .isInstanceOf(ChatRoomException.class)
                    .hasMessage(ERROR_NO_DELETE_PERMISSION);

            verify(chatRoomRepository).findById(CHAT_ROOM_ID);
            verify(participantRepo).findByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID);
            verify(chatRoomRepository, never()).delete(any());
        }

        @Test
        @DisplayName("참여하지 않은 사용자가 채팅방 삭제 시 권한 예외가 발생해야 함")
        void givenNonParticipantUser_whenDeleteChatRoom_thenThrowChatRoomException() {
            // Given
            when(chatRoomRepository.findById(CHAT_ROOM_ID)).thenReturn(Optional.of(testChatRoom));
            when(participantRepo.findByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatRoomService.deleteChatRoom(CHAT_ROOM_ID, USER_ID))
                    .isInstanceOf(ChatRoomException.class)
                    .hasMessage(ERROR_NO_DELETE_PERMISSION);

            verify(chatRoomRepository).findById(CHAT_ROOM_ID);
            verify(participantRepo).findByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID);
            verify(chatRoomRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("채팅방 조회 테스트")
    class FindChatRoomTests {

        @Test
        @DisplayName("사용자의 채팅방 목록 조회 시 해당 사용자가 참여한 채팅방들이 반환되어야 함")
        void givenUserId_whenFindChatRoomsByUser_thenReturnUserChatRooms() {
            // Given
            List<ChatRoom> userChatRooms = List.of(testChatRoom);
            List<ChatRoomResponse> expectedResponses = List.of(testChatRoomResponse);

            doNothing().when(entityFinderService).validateUserExists(USER_ID);
            when(chatRoomRepository.findAllByParticipantUserId(USER_ID)).thenReturn(userChatRooms);
            when(chatRoomMapper.toResponse(testChatRoom)).thenReturn(testChatRoomResponse);

            // When
            List<ChatRoomResponse> result = chatRoomService.findChatRoomsByUser(USER_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(CHAT_ROOM_ID);

            verify(entityFinderService).validateUserExists(USER_ID);
            verify(chatRoomRepository).findAllByParticipantUserId(USER_ID);
            verify(chatRoomMapper).toResponse(testChatRoom);
        }

        @Test
        @DisplayName("ID로 채팅방 조회 시 해당 채팅방이 반환되어야 함")
        void givenChatRoomId_whenFindChatRoomById_thenReturnChatRoom() {
            // Given
            when(chatRoomRepository.findByIdWithParticipants(CHAT_ROOM_ID))
                    .thenReturn(Optional.of(testChatRoom));
            when(chatRoomMapper.toResponse(testChatRoom)).thenReturn(testChatRoomResponse);

            // When
            Optional<ChatRoomResponse> result = chatRoomService.findChatRoomById(CHAT_ROOM_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(CHAT_ROOM_ID);

            verify(chatRoomRepository).findByIdWithParticipants(CHAT_ROOM_ID);
            verify(chatRoomMapper).toResponse(testChatRoom);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 채팅방 조회 시 빈 Optional이 반환되어야 함")
        void givenNonExistentId_whenFindChatRoomById_thenReturnEmpty() {
            // Given
            when(chatRoomRepository.findByIdWithParticipants(NONEXISTENT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<ChatRoomResponse> result = chatRoomService.findChatRoomById(NONEXISTENT_ID);

            // Then
            assertThat(result).isEmpty();

            verify(chatRoomRepository).findByIdWithParticipants(NONEXISTENT_ID);
            verify(chatRoomMapper, never()).toResponse(any());
        }
    }
}
