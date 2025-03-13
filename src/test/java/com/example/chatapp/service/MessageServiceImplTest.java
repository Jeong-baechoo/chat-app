package com.example.chatapp.service;

import com.example.chatapp.domain.*;
import com.example.chatapp.dto.MessageDTO;
import com.example.chatapp.dto.MessageRequestDTO;
import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @InjectMocks
    private MessageServiceImpl messageServiceImpl;

    private User testUser;
    private ChatRoom testChatRoom;
    private Message testMessage;
    private MessageCreateRequest validMessageRequest;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 설정
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // 테스트 채팅방 설정
        testChatRoom = new ChatRoom();
        testChatRoom.setId(1L);
        testChatRoom.setName("Test Room");

        // 테스트 메시지 설정
        testMessage = Message.builder()
                .id(1L)
                .sender(testUser)
                .chatRoom(testChatRoom)
                .content("테스트 메시지")
                .status(MessageStatus.SENT)
                .timestamp(LocalDateTime.now())
                .build();

        // 유효한 메시지 요청 DTO 설정
        validMessageRequest = new MessageCreateRequest();
        validMessageRequest.setSenderId(1L);
        validMessageRequest.setChatRoomId(1L);
        validMessageRequest.setContent("테스트 메시지");
    }

    @Test
    @DisplayName("메시지 전송 성공")
    void sendMessage_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatRoomParticipantRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // When
        MessageResponse result = messageServiceImpl.sendMessage(validMessageRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("테스트 메시지", result.getContent());

        // 저장소 호출 검증
        verify(userRepository).findById(1L);
        verify(chatRoomRepository).findById(1L);
        verify(chatRoomParticipantRepository).existsByUserIdAndChatRoomId(1L, 1L);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());

        Message capturedMessage = messageCaptor.getValue();
        assertEquals("테스트 메시지", capturedMessage.getContent());
        assertEquals(MessageStatus.SENT, capturedMessage.getStatus());
    }

    @Test
    @DisplayName("메시지 전송 실패 - 빈 메시지 내용")
    void sendMessage_EmptyContent() {
        // Given
        MessageCreateRequest emptyRequest = new MessageCreateRequest();
        emptyRequest.setSenderId(1L);
        emptyRequest.setChatRoomId(1L);
        emptyRequest.setContent("");

        // When & Then
        assertThrows(MessageException.class, () -> messageServiceImpl.sendMessage(emptyRequest));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("메시지 전송 실패 - 최대 길이 초과")
    void sendMessage_ContentTooLong() {
        // Given
        MessageCreateRequest longRequest = new MessageCreateRequest();
        longRequest.setSenderId(1L);
        longRequest.setChatRoomId(1L);
        longRequest.setContent("a".repeat(1001)); // 1001자 (최대 1000자)

        // When & Then
        assertThrows(MessageException.class, () -> messageServiceImpl.sendMessage(longRequest));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("메시지 전송 실패 - 사용자 없음")
    void sendMessage_UserNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserException.class, () -> messageServiceImpl.sendMessage(validMessageRequest));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("메시지 전송 실패 - 채팅방 없음")
    void sendMessage_ChatRoomNotFound() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ChatRoomException.class, () -> messageServiceImpl.sendMessage(validMessageRequest));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("메시지 전송 실패 - 채팅방 참여자 아님")
    void sendMessage_NotParticipant() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
        when(chatRoomParticipantRepository.existsByUserIdAndChatRoomId(1L, 1L)).thenReturn(false);

        // When & Then
        assertThrows(MessageException.class, () -> messageServiceImpl.sendMessage(validMessageRequest));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("채팅방 메시지 조회 성공")
    void getChatRoomMessages_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> messages = Arrays.asList(testMessage);
        Page<Message> messagePage = new PageImpl<>(messages);

        when(chatRoomRepository.existsById(1L)).thenReturn(true);
        when(messageRepository.findByChatRoomIdOrderByTimestampDesc(eq(1L), any(Pageable.class)))
                .thenReturn(messagePage);

        // When
        Page<MessageResponse> result = messageServiceImpl.getChatRoomMessages(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
    }

    @Test
    @DisplayName("채팅방 메시지 조회 실패 - 채팅방 없음")
    void getChatRoomMessages_ChatRoomNotFound() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(chatRoomRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThrows(ChatRoomException.class, () -> messageServiceImpl.getChatRoomMessages(1L, pageable));
    }

    @Test
    @DisplayName("최근 채팅방 메시지 조회 성공")
    void getRecentChatRoomMessages_Success() {
        // Given
        List<Message> messages = Arrays.asList(testMessage);
        when(chatRoomRepository.existsById(1L)).thenReturn(true);
        when(messageRepository.findTopByChatRoomIdOrderByTimestampDesc(eq(1L), anyInt()))
                .thenReturn(messages);

        // When
        List<MessageResponse> result = messageServiceImpl.getRecentChatRoomMessages(1L, 10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    @DisplayName("메시지 ID로 조회 성공")
    void findMessageById_Success() {
        // Given
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));

        // When
        MessageResponse result = messageServiceImpl.findMessageById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("테스트 메시지", result.getContent());
    }

    @Test
    @DisplayName("메시지 ID로 조회 실패 - 메시지 없음")
    void findMessageById_NotFound() {
        // Given
        when(messageRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(MessageException.class, () -> messageServiceImpl.findMessageById(999L));
    }

    @Test
    @DisplayName("메시지 상태 업데이트 성공")
    void updateMessageStatus_Success() {
        // Given
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // When
        MessageResponse result = messageServiceImpl.updateMessageStatus(1L, 1L, MessageStatus.READ);

        // Then
        assertNotNull(result);
        verify(messageRepository).save(any(Message.class));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertEquals(MessageStatus.READ, messageCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("메시지 상태 업데이트 실패 - 권한 없음")
    void updateMessageStatus_NoPermission() {
        // Given
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));

        // When & Then
        assertThrows(MessageException.class, () -> messageServiceImpl.updateMessageStatus(1L, 2L, MessageStatus.READ));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("발신자별 메시지 조회 성공")
    void getMessagesBySender_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> messages = Arrays.asList(testMessage);
        Page<Message> messagePage = new PageImpl<>(messages);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(messageRepository.findBySenderIdOrderByTimestampDesc(eq(1L), any(Pageable.class)))
                .thenReturn(messagePage);

        // When
        Page<MessageResponse> result = messageServiceImpl.getMessagesBySender(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
    }

    @Test
    @DisplayName("메시지 삭제 성공")
    void deleteMessage_Success() {
        // Given
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));

        // When
        messageServiceImpl.deleteMessage(1L, 1L);

        // Then
        verify(messageRepository).delete(testMessage);
    }

    @Test
    @DisplayName("메시지 삭제 실패 - 권한 없음")
    void deleteMessage_NoPermission() {
        // Given
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));

        // When & Then
        assertThrows(MessageException.class, () -> messageServiceImpl.deleteMessage(1L, 2L));
        verify(messageRepository, never()).delete(any());
    }
}
