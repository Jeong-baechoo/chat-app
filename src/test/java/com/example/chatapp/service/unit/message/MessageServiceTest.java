package com.example.chatapp.service.unit.message;

import com.example.chatapp.domain.*;
import com.example.chatapp.domain.service.MessageDomainService;
import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.domain.exception.MessageDomainException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.infrastructure.event.ChatEventPublisherService;
import com.example.chatapp.mapper.MessageMapper;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CHAT_ROOM_ID = 1L;
    private static final Long MESSAGE_ID = 1L;
    private static final Long NONEXISTENT_ID = 999L;
    private static final String TEST_MESSAGE_CONTENT = "테스트 메시지";

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageDomainService messageDomainService;


    @Mock
    private MessageMapper messageMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;


    @Mock
    private ChatEventPublisherService eventPublisher;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User testUser;
    private ChatRoom testChatRoom;
    private Message testMessage;
    private ChatRoomParticipant testParticipant;
    private MessageCreateRequest validMessageRequest;
    private MessageResponse messageResponse;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 설정
        testUser = createTestUser(USER_ID, "testUser");

        // 테스트 채팅방 설정 (사용자가 참여자로 추가됨)
        testChatRoom = createTestChatRoom(CHAT_ROOM_ID, "Test Room");
        
        // 테스트 참가자 설정 (채팅방에 사용자 추가)
        testParticipant = createTestParticipant(1L, testUser, testChatRoom, ParticipantRole.MEMBER);
        // 채팅방의 participants 리스트에 추가 (리플렉션 사용)
        addParticipantToChatRoom(testChatRoom, testParticipant);

        // 테스트 메시지 설정 - Message.create()를 사용하지 않고 직접 생성
        testMessage = createTestMessageWithoutValidation(MESSAGE_ID, TEST_MESSAGE_CONTENT, testUser, testChatRoom);

        // 유효한 메시지 요청 DTO 설정
        validMessageRequest = new MessageCreateRequest();
        validMessageRequest.setChatRoomId(CHAT_ROOM_ID);
        validMessageRequest.setContent(TEST_MESSAGE_CONTENT);

        // 메시지 응답 DTO 설정
        messageResponse = MessageResponse.builder()
                .id(MESSAGE_ID)
                .content(TEST_MESSAGE_CONTENT)
                .sender(new UserResponse(USER_ID, "testUser"))
                .chatRoomId(CHAT_ROOM_ID)
                .status(MessageStatus.SENT)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 테스트용 헬퍼 메서드들
    private User createTestUser(Long id, String username) {
        User user = User.create(username, "encoded_password");
        setField(user, "id", id);
        return user;
    }

    private ChatRoom createTestChatRoom(Long id, String name) {
        // ChatRoom의 private 생성자를 리플렉션으로 호출하여 participants 초기화 없이 생성
        try {
            java.lang.reflect.Constructor<ChatRoom> constructor = ChatRoom.class.getDeclaredConstructor(ChatRoomName.class, ChatRoomType.class);
            constructor.setAccessible(true);
            ChatRoom chatRoom = constructor.newInstance(ChatRoomName.of(name), ChatRoomType.GROUP);
            setField(chatRoom, "id", id);
            // participants 필드가 null이 아닌지 확인하고 필요시 초기화
            ensureParticipantsInitialized(chatRoom);
            return chatRoom;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test chat room", e);
        }
    }
    
    private void ensureParticipantsInitialized(ChatRoom chatRoom) {
        try {
            java.lang.reflect.Field field = chatRoom.getClass().getDeclaredField("participants");
            field.setAccessible(true);
            if (field.get(chatRoom) == null) {
                field.set(chatRoom, new java.util.HashSet<>());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure participants initialized", e);
        }
    }

    private Message createTestMessage(Long id, String content, User sender, ChatRoom chatRoom) {
        Message message = Message.create(content, sender, chatRoom);
        setField(message, "id", id);
        return message;
    }
    
    private Message createTestMessageWithoutValidation(Long id, String content, User sender, ChatRoom chatRoom) {
        // Message의 private 생성자를 리플렉션으로 호출
        try {
            java.lang.reflect.Constructor<Message> constructor = Message.class.getDeclaredConstructor(String.class, User.class, ChatRoom.class);
            constructor.setAccessible(true);
            Message message = constructor.newInstance(content, sender, chatRoom);
            setField(message, "id", id);
            return message;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test message", e);
        }
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
    
    // 채팅방에 참가자 추가 (테스트용)
    private void addParticipantToChatRoom(ChatRoom chatRoom, ChatRoomParticipant participant) {
        try {
            java.lang.reflect.Field field = chatRoom.getClass().getDeclaredField("participants");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Set<ChatRoomParticipant> participants = (java.util.Set<ChatRoomParticipant>) field.get(chatRoom);
            if (participants == null) {
                participants = new java.util.HashSet<>();
                field.set(chatRoom, participants);
            }
            participants.add(participant);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add participant to chat room", e);
        }
    }

    @Nested
    @DisplayName("메시지 전송 테스트")
    class SendMessageTests {

        @Test
        @DisplayName("givenValidRequest_whenSendMessage_thenMessageSaved")
        void givenValidRequest_whenSendMessage_thenMessageSaved() {
            // Given
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(CHAT_ROOM_ID)).thenReturn(Optional.of(testChatRoom));
            when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

            // When
            messageService.sendMessage(validMessageRequest, USER_ID);

            // Then - void 메서드이므로 리턴값 검증 대신 동작 검증
            verify(userRepository).findById(USER_ID);
            verify(chatRoomRepository).findById(CHAT_ROOM_ID);
            verify(messageRepository).save(any(Message.class));
            verify(eventPublisher).publishMessageEvent(any(Message.class), eq(testUser));
        }

        @Test
        @DisplayName("givenNonExistentUser_whenSendMessage_thenThrowUserException")
        void givenNonExistentUser_whenSendMessage_thenThrowUserException() {
            // Given
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> messageService.sendMessage(validMessageRequest, USER_ID))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");

            // 검증
            verify(userRepository).findById(USER_ID);
            verify(messageRepository, never()).save(any(Message.class));
            verify(eventPublisher, never()).publishMessageEvent(any(), any());
        }
        
        @Test
        @DisplayName("givenNonExistentChatRoom_whenSendMessage_thenThrowChatRoomException")
        void givenNonExistentChatRoom_whenSendMessage_thenThrowChatRoomException() {
            // Given
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(chatRoomRepository.findById(CHAT_ROOM_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> messageService.sendMessage(validMessageRequest, USER_ID))
                    .isInstanceOf(ChatRoomException.class)
                    .hasMessageContaining("채팅방을 찾을 수 없습니다");

            // 검증
            verify(userRepository).findById(USER_ID);
            verify(chatRoomRepository).findById(CHAT_ROOM_ID);
            verify(messageRepository, never()).save(any(Message.class));
            verify(eventPublisher, never()).publishMessageEvent(any(), any());
        }
    }

    @Nested
    @DisplayName("메시지 조회 테스트")
    class GetMessagesTests {

        @Test
        @DisplayName("givenChatRoomId_whenGetChatRoomMessages_thenReturnMessagePage")
        void givenChatRoomId_whenGetChatRoomMessages_thenReturnMessagePage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Message> messages = Arrays.asList(testMessage);
            Page<Message> messagePage = new PageImpl<>(messages);

            when(chatRoomRepository.existsById(CHAT_ROOM_ID)).thenReturn(true);
            when(messageRepository.findByChatRoomIdOrderByTimestampDesc(CHAT_ROOM_ID, pageable))
                    .thenReturn(messagePage);
            when(messageMapper.toResponse(testMessage)).thenReturn(messageResponse);

            // When
            Page<MessageResponse> result = messageService.findChatRoomMessages(CHAT_ROOM_ID, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(MESSAGE_ID);

            verify(chatRoomRepository).existsById(CHAT_ROOM_ID);
            verify(messageRepository).findByChatRoomIdOrderByTimestampDesc(CHAT_ROOM_ID, pageable);
        }

        @Test
        @DisplayName("givenChatRoomIdAndLimit_whenFindRecentChatRoomMessages_thenReturnMessageList")
        void givenChatRoomIdAndLimit_whenFindRecentChatRoomMessages_thenReturnMessageList() {
            // Given
            int limit = 10;
            List<Message> messages = List.of(testMessage);

            when(chatRoomRepository.existsById(CHAT_ROOM_ID)).thenReturn(true);
            when(messageRepository.findTopByChatRoomIdWithSenderAndRoomOrderByTimestampDesc(CHAT_ROOM_ID, limit))
                    .thenReturn(messages);
            when(messageMapper.toResponse(testMessage)).thenReturn(messageResponse);

            // When
            List<MessageResponse> result = messageService.findRecentChatRoomMessages(CHAT_ROOM_ID, limit);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(MESSAGE_ID);

            verify(chatRoomRepository).existsById(CHAT_ROOM_ID);
            verify(messageRepository).findTopByChatRoomIdWithSenderAndRoomOrderByTimestampDesc(CHAT_ROOM_ID, limit);
        }

        @Test
        @DisplayName("givenMessageId_whenFindMessageById_thenReturnMessageResponse")
        void givenMessageId_whenFindMessageById_thenReturnMessageResponse() {
            // Given
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(testMessage));
            when(messageMapper.toResponse(testMessage)).thenReturn(messageResponse);

            // When
            MessageResponse result = messageService.findMessageById(MESSAGE_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(MESSAGE_ID);
            assertThat(result.getContent()).isEqualTo(TEST_MESSAGE_CONTENT);

            verify(messageRepository).findById(MESSAGE_ID);
        }

        @Test
        @DisplayName("givenSenderId_whenGetMessagesBySender_thenReturnMessagePage")
        void givenSenderId_whenGetMessagesBySender_thenReturnMessagePage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Message> messages = Arrays.asList(testMessage);
            Page<Message> messagePage = new PageImpl<>(messages);

            when(userRepository.existsById(USER_ID)).thenReturn(true);
            when(messageRepository.findBySenderIdWithSenderAndRoomOrderByTimestampDesc(USER_ID, pageable))
                    .thenReturn(messagePage);
            when(messageMapper.toResponse(testMessage)).thenReturn(messageResponse);

            // When
            Page<MessageResponse> result = messageService.findMessagesBySender(USER_ID, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(MESSAGE_ID);

            verify(userRepository).existsById(USER_ID);
            verify(messageRepository).findBySenderIdWithSenderAndRoomOrderByTimestampDesc(USER_ID, pageable);
        }

        @Test
        @DisplayName("givenNonExistentChatRoom_whenGetChatRoomMessages_thenThrowChatRoomException")
        void givenNonExistentChatRoom_whenGetChatRoomMessages_thenThrowChatRoomException() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            when(chatRoomRepository.existsById(NONEXISTENT_ID)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> messageService.findChatRoomMessages(NONEXISTENT_ID, pageable))
                    .isInstanceOf(ChatRoomException.class)
                    .hasMessageContaining("채팅방을 찾을 수 없습니다");

            verify(messageRepository, never()).findByChatRoomIdOrderByTimestampDesc(anyLong(), any());
        }

        @Test
        @DisplayName("givenNonExistentChatRoom_whenGetRecentChatRoomMessages_thenThrowChatRoomException")
        void givenNonExistentChatRoom_whenGetRecentChatRoomMessages_thenThrowChatRoomException() {
            // Given
            int limit = 10;
            when(chatRoomRepository.existsById(NONEXISTENT_ID)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> messageService.findRecentChatRoomMessages(NONEXISTENT_ID, limit))
                    .isInstanceOf(ChatRoomException.class)
                    .hasMessageContaining("채팅방을 찾을 수 없습니다");

            verify(messageRepository, never()).findTopByChatRoomIdWithSenderAndRoomOrderByTimestampDesc(anyLong(), anyInt());
        }
    }

    @Nested
    @DisplayName("메시지 상태 업데이트 테스트")
    class UpdateMessageStatusTests {

        @Test
        @DisplayName("givenMessageAndUser_whenUpdateMessageStatus_thenReturnUpdatedMessage")
        void givenMessageAndUser_whenUpdateMessageStatus_thenReturnUpdatedMessage() {
            // Given
            Message updatedMessage = createTestMessage(MESSAGE_ID, TEST_MESSAGE_CONTENT, testUser, testChatRoom);
            setField(updatedMessage, "status", MessageStatus.READ); // 상태 변경됨

            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(testMessage));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(messageDomainService.canUserUpdateMessage(testUser, testMessage)).thenReturn(true);
            when(messageRepository.save(any(Message.class))).thenReturn(updatedMessage);

            MessageResponse updatedResponse = MessageResponse.builder()
                    .id(MESSAGE_ID)
                    .content(TEST_MESSAGE_CONTENT)
                    .sender(new UserResponse(USER_ID, "testUser"))
                    .chatRoomId(CHAT_ROOM_ID)
                    .status(MessageStatus.READ) // 변경된 상태
                    .timestamp(LocalDateTime.now())
                    .build();

            when(messageMapper.toResponse(updatedMessage)).thenReturn(updatedResponse);

            // When
            MessageResponse result = messageService.updateMessageStatus(MESSAGE_ID, USER_ID, MessageStatus.READ);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(MessageStatus.READ);

            verify(messageRepository).findById(MESSAGE_ID);
            verify(userRepository).findById(USER_ID);
            verify(messageDomainService).canUserUpdateMessage(testUser, testMessage);
            verify(messageRepository).save(any(Message.class));
        }

        @Test
        @DisplayName("givenUnauthorizedUser_whenUpdateMessageStatus_thenThrowMessageException")
        void givenUnauthorizedUser_whenUpdateMessageStatus_thenThrowMessageException() {
            // Given
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(testMessage));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(messageDomainService.canUserUpdateMessage(testUser, testMessage)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> messageService.updateMessageStatus(MESSAGE_ID, USER_ID, MessageStatus.READ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage("메시지 접근 권한이 없습니다");

            verify(messageDomainService).canUserUpdateMessage(testUser, testMessage);
            verify(messageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("메시지 삭제 테스트")
    class DeleteMessageTests {

        @Test
        @DisplayName("givenAuthorizedUser_whenDeleteMessage_thenMessageDeleted")
        void givenAuthorizedUser_whenDeleteMessage_thenMessageDeleted() {
            // Given
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(testMessage));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(messageDomainService).validateDeletePermission(testUser, testMessage, testChatRoom);

            // When
            messageService.deleteMessage(MESSAGE_ID, USER_ID);

            // Then
            verify(messageRepository).findById(MESSAGE_ID);
            verify(userRepository).findById(USER_ID);
            verify(messageDomainService).validateDeletePermission(testUser, testMessage, testChatRoom);
            verify(messageRepository).delete(testMessage);
        }

        @Test
        @DisplayName("givenUnauthorizedUser_whenDeleteMessage_thenThrowMessageException")
        void givenUnauthorizedUser_whenDeleteMessage_thenThrowMessageException() {
            // Given
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(testMessage));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            doThrow(new MessageDomainException("메시지를 삭제할 권한이 없습니다"))
                    .when(messageDomainService).validateDeletePermission(testUser, testMessage, testChatRoom);

            // When & Then
            assertThatThrownBy(() -> messageService.deleteMessage(MESSAGE_ID, USER_ID))
                    .isInstanceOf(MessageDomainException.class)
                    .hasMessageContaining("메시지를 삭제할 권한이 없습니다");

            verify(messageDomainService).validateDeletePermission(testUser, testMessage, testChatRoom);
            verify(messageRepository, never()).delete(any());
        }
    }
}
