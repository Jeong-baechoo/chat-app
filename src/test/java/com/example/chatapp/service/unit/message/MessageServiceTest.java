package com.example.chatapp.service.unit.message;

import com.example.chatapp.domain.*;
import com.example.chatapp.domain.service.MessageDomainService;
import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.infrastructure.event.ChatEventPublisherService;
import com.example.chatapp.mapper.MessageMapper;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.service.EntityFinderService;
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
    private static final String ERROR_NOT_PARTICIPANT = "채팅방 참여자만 메시지를 보낼 수 있습니다";
    private static final String ERROR_NO_PERMISSION = "메시지 상태를 변경할 권한이 없습니다";
    private static final String ERROR_NO_DELETE_PERMISSION = "메시지 삭제 권한이 없습니다";
    private static final String ERROR_CHAT_ROOM_NOT_FOUND = "채팅방을 찾을 수 없습니다";

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageDomainService messageDomainService;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private EntityFinderService entityFinder;


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

        // 테스트 채팅방 설정
        testChatRoom = createTestChatRoom(CHAT_ROOM_ID, "Test Room");

        // 테스트 메시지 설정
        testMessage = createTestMessage(MESSAGE_ID, TEST_MESSAGE_CONTENT, testUser, testChatRoom);

        // 테스트 참가자 설정
        testParticipant = createTestParticipant(1L, testUser, testChatRoom, ParticipantRole.MEMBER);

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
        User tempUser = User.create("temp", "encoded_password");
        ChatRoom chatRoom = ChatRoom.create(name, ChatRoomType.GROUP, tempUser);
        setField(chatRoom, "id", id);
        return chatRoom;
    }

    private Message createTestMessage(Long id, String content, User sender, ChatRoom chatRoom) {
        Message message = Message.builder()
                .sender(sender)
                .chatRoom(chatRoom)
                .content(content)
                .status(MessageStatus.SENT)
                .timestamp(LocalDateTime.now())
                .build();
        setField(message, "id", id);
        return message;
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
    @DisplayName("메시지 전송 테스트")
    class SendMessageTests {

        @Test
        @DisplayName("givenValidRequest_whenSendMessage_thenMessageSaved")
        void givenValidRequest_whenSendMessage_thenMessageSaved() {
            // Given
            when(chatRoomParticipantRepository.findByUserIdAndChatRoomIdWithUserAndChatRoom(USER_ID, CHAT_ROOM_ID))
                    .thenReturn(Optional.of(testParticipant));
            when(messageDomainService.createMessage(TEST_MESSAGE_CONTENT, testUser, testChatRoom)).thenReturn(testMessage);
            when(messageRepository.save(testMessage)).thenReturn(testMessage);

            // When
            messageService.sendMessage(validMessageRequest, USER_ID);

            // Then - void 메서드이므로 리턴값 검증 대신 동작 검증
            verify(validator).validateMessageRequest(validMessageRequest);
            verify(chatRoomParticipantRepository).findByUserIdAndChatRoomIdWithUserAndChatRoom(USER_ID, CHAT_ROOM_ID);
            verify(messageDomainService).createMessage(TEST_MESSAGE_CONTENT, testUser, testChatRoom);
            verify(messageRepository).save(testMessage);
            verify(eventPublisher).publishMessageEvent(testMessage, testUser);
        }

        @Test
        @DisplayName("givenNonParticipant_whenSendMessage_thenThrowMessageException")
        void givenNonParticipant_whenSendMessage_thenThrowMessageException() {
            // Given
            when(chatRoomParticipantRepository.findByUserIdAndChatRoomIdWithUserAndChatRoom(USER_ID, CHAT_ROOM_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> messageService.sendMessage(validMessageRequest, USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(ERROR_NOT_PARTICIPANT);

            // 검증
            verify(validator).validateMessageRequest(validMessageRequest);
            verify(chatRoomParticipantRepository).findByUserIdAndChatRoomIdWithUserAndChatRoom(USER_ID, CHAT_ROOM_ID);
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

            doNothing().when(entityFinder).validateChatRoomExists(CHAT_ROOM_ID);
            when(messageRepository.findByChatRoomIdOrderByTimestampDesc(CHAT_ROOM_ID, pageable))
                    .thenReturn(messagePage);
            when(messageMapper.toResponse(testMessage)).thenReturn(messageResponse);

            // When
            Page<MessageResponse> result = messageService.findChatRoomMessages(CHAT_ROOM_ID, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(MESSAGE_ID);

            verify(entityFinder).validateChatRoomExists(CHAT_ROOM_ID);
            verify(messageRepository).findByChatRoomIdOrderByTimestampDesc(CHAT_ROOM_ID, pageable);
        }

        @Test
        @DisplayName("givenChatRoomIdAndLimit_whenFindRecentChatRoomMessages_thenReturnMessageList")
        void givenChatRoomIdAndLimit_whenFindRecentChatRoomMessages_thenReturnMessageList() {
            // Given
            int limit = 10;
            List<Message> messages = List.of(testMessage);

            doNothing().when(entityFinder).validateChatRoomExists(CHAT_ROOM_ID);
            when(messageRepository.findTopByChatRoomIdWithSenderAndRoomOrderByTimestampDesc(CHAT_ROOM_ID, limit))
                    .thenReturn(messages);
            when(messageMapper.toResponse(testMessage)).thenReturn(messageResponse);

            // When
            List<MessageResponse> result = messageService.findRecentChatRoomMessages(CHAT_ROOM_ID, limit);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(MESSAGE_ID);

            verify(entityFinder).validateChatRoomExists(CHAT_ROOM_ID);
            verify(messageRepository).findTopByChatRoomIdWithSenderAndRoomOrderByTimestampDesc(CHAT_ROOM_ID, limit);
        }

        @Test
        @DisplayName("givenMessageId_whenFindMessageById_thenReturnMessageResponse")
        void givenMessageId_whenFindMessageById_thenReturnMessageResponse() {
            // Given
            when(entityFinder.findMessageById(MESSAGE_ID)).thenReturn(testMessage);
            when(messageMapper.toResponse(testMessage)).thenReturn(messageResponse);

            // When
            MessageResponse result = messageService.findMessageById(MESSAGE_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(MESSAGE_ID);
            assertThat(result.getContent()).isEqualTo(TEST_MESSAGE_CONTENT);

            verify(entityFinder).findMessageById(MESSAGE_ID);
        }

        @Test
        @DisplayName("givenSenderId_whenGetMessagesBySender_thenReturnMessagePage")
        void givenSenderId_whenGetMessagesBySender_thenReturnMessagePage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<Message> messages = Arrays.asList(testMessage);
            Page<Message> messagePage = new PageImpl<>(messages);

            when(messageRepository.findBySenderIdWithSenderAndRoomOrderByTimestampDesc(USER_ID, pageable))
                    .thenReturn(messagePage);
            when(messageMapper.toResponse(testMessage)).thenReturn(messageResponse);

            // When
            Page<MessageResponse> result = messageService.findMessagesBySender(USER_ID, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().getFirst().getId()).isEqualTo(MESSAGE_ID);

            verify(entityFinder).validateUserExists(USER_ID);
            verify(messageRepository).findBySenderIdWithSenderAndRoomOrderByTimestampDesc(USER_ID, pageable);
        }

        @Test
        @DisplayName("givenNonExistentChatRoom_whenGetChatRoomMessages_thenThrowChatRoomException")
        void givenNonExistentChatRoom_whenGetChatRoomMessages_thenThrowChatRoomException() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            doThrow(new ChatRoomException(ERROR_CHAT_ROOM_NOT_FOUND))
                    .when(entityFinder).validateChatRoomExists(NONEXISTENT_ID);

            // When & Then
            assertThatThrownBy(() -> messageService.findChatRoomMessages(NONEXISTENT_ID, pageable))
                    .isInstanceOf(ChatRoomException.class)
                    .hasMessage(ERROR_CHAT_ROOM_NOT_FOUND);

            verify(messageRepository, never()).findByChatRoomIdOrderByTimestampDesc(anyLong(), any());
        }

        @Test
        @DisplayName("givenNonExistentChatRoom_whenGetRecentChatRoomMessages_thenThrowChatRoomException")
        void givenNonExistentChatRoom_whenGetRecentChatRoomMessages_thenThrowChatRoomException() {
            // Given
            int limit = 10;
            doThrow(new ChatRoomException(ERROR_CHAT_ROOM_NOT_FOUND))
                    .when(entityFinder).validateChatRoomExists(NONEXISTENT_ID);

            // When & Then
            assertThatThrownBy(() -> messageService.findRecentChatRoomMessages(NONEXISTENT_ID, limit))
                    .isInstanceOf(ChatRoomException.class)
                    .hasMessage(ERROR_CHAT_ROOM_NOT_FOUND);

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

            when(entityFinder.findMessageById(MESSAGE_ID)).thenReturn(testMessage);
            when(entityFinder.findUserById(USER_ID)).thenReturn(testUser);
            when(messageDomainService.canUserUpdateMessage(testUser, testMessage)).thenReturn(true);
            when(messageDomainService.updateMessageStatus(testMessage, MessageStatus.READ)).thenReturn(updatedMessage);
            when(messageRepository.save(updatedMessage)).thenReturn(updatedMessage);

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

            verify(entityFinder).findMessageById(MESSAGE_ID);
            verify(entityFinder).findUserById(USER_ID);
            verify(messageDomainService).canUserUpdateMessage(testUser, testMessage);
            verify(messageDomainService).updateMessageStatus(testMessage, MessageStatus.READ);
            verify(messageRepository).save(updatedMessage);
        }

        @Test
        @DisplayName("givenUnauthorizedUser_whenUpdateMessageStatus_thenThrowMessageException")
        void givenUnauthorizedUser_whenUpdateMessageStatus_thenThrowMessageException() {
            // Given
            when(entityFinder.findMessageById(MESSAGE_ID)).thenReturn(testMessage);
            when(entityFinder.findUserById(USER_ID)).thenReturn(testUser);
            when(messageDomainService.canUserUpdateMessage(testUser, testMessage)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> messageService.updateMessageStatus(MESSAGE_ID, USER_ID, MessageStatus.READ))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(ERROR_NO_PERMISSION);

            verify(messageDomainService).canUserUpdateMessage(testUser, testMessage);
            verify(messageDomainService, never()).updateMessageStatus(any(), any());
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
            when(entityFinder.findMessageById(MESSAGE_ID)).thenReturn(testMessage);
            when(entityFinder.findUserById(USER_ID)).thenReturn(testUser);
            doNothing().when(messageDomainService).validateDeletePermission(testUser, testMessage, testChatRoom);

            // When
            messageService.deleteMessage(MESSAGE_ID, USER_ID);

            // Then
            verify(entityFinder).findMessageById(MESSAGE_ID);
            verify(entityFinder).findUserById(USER_ID);
            verify(messageDomainService).validateDeletePermission(testUser, testMessage, testChatRoom);
            verify(messageRepository).delete(testMessage);
        }

        @Test
        @DisplayName("givenUnauthorizedUser_whenDeleteMessage_thenThrowMessageException")
        void givenUnauthorizedUser_whenDeleteMessage_thenThrowMessageException() {
            // Given
            when(entityFinder.findMessageById(MESSAGE_ID)).thenReturn(testMessage);
            when(entityFinder.findUserById(USER_ID)).thenReturn(testUser);
            doThrow(new MessageException(ERROR_NO_DELETE_PERMISSION))
                    .when(messageDomainService).validateDeletePermission(testUser, testMessage, testChatRoom);

            // When & Then
            assertThatThrownBy(() -> messageService.deleteMessage(MESSAGE_ID, USER_ID))
                    .isInstanceOf(MessageException.class)
                    .hasMessage(ERROR_NO_DELETE_PERMISSION);

            verify(messageDomainService).validateDeletePermission(testUser, testMessage, testChatRoom);
            verify(messageRepository, never()).delete(any());
        }
    }
}
