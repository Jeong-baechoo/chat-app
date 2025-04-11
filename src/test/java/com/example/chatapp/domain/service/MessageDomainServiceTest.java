package com.example.chatapp.domain.service;

import com.example.chatapp.domain.*;
import com.example.chatapp.exception.MessageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class MessageDomainServiceTest {

    private MessageDomainService messageDomainService;
    private User user, otherUser, adminUser;
    private ChatRoom chatRoom;
    private Message message;
    private ChatRoomParticipant participant, adminParticipant;

    @BeforeEach
    void setUp() {
        messageDomainService = new MessageDomainService();

        // 사용자 설정
        user = User.builder()
                .id(1L)
                .username("user")
                .build();

        otherUser = User.builder()
                .id(2L)
                .username("otherUser")
                .build();

        adminUser = User.builder()
                .id(3L)
                .username("adminUser")
                .build();

        // 채팅방 참여자 설정
        participant = new ChatRoomParticipant();
        participant.setUser(user);
        participant.setRole(ParticipantRole.MEMBER);

        adminParticipant = new ChatRoomParticipant();
        adminParticipant.setUser(adminUser);
        adminParticipant.setRole(ParticipantRole.ADMIN);

        // 채팅방 설정
        chatRoom = new ChatRoom();
        chatRoom.setId(1L);
        chatRoom.setName("Test Room");

        // 채팅방 참여자 설정
        HashSet<ChatRoomParticipant> participants = new HashSet<>();
        participants.add(participant);
        participants.add(adminParticipant);
        chatRoom.setParticipants(participants);

        // 메시지 설정
        message = Message.builder()
                .id(1L)
                .sender(user)
                .chatRoom(chatRoom)
                .content("테스트 메시지")
                .status(MessageStatus.SENT)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("사용자가 메시지를 전송할 수 있는지 확인 - 참여자인 경우")
    void canUserSendMessage_WhenParticipant_ShouldReturnTrue() {
        // When
        boolean result = messageDomainService.canUserSendMessage(user, chatRoom);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("사용자가 메시지를 전송할 수 있는지 확인 - 참여자가 아닌 경우")
    void canUserSendMessage_WhenNotParticipant_ShouldReturnFalse() {
        // When
        boolean result = messageDomainService.canUserSendMessage(otherUser, chatRoom);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("사용자가 메시지를 업데이트할 수 있는지 확인 - 발신자인 경우")
    void canUserUpdateMessage_WhenSender_ShouldReturnTrue() {
        // When
        boolean result = messageDomainService.canUserUpdateMessage(user, message);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("사용자가 메시지를 업데이트할 수 있는지 확인 - 발신자가 아닌 경우")
    void canUserUpdateMessage_WhenNotSender_ShouldReturnFalse() {
        // When
        boolean result = messageDomainService.canUserUpdateMessage(otherUser, message);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("사용자가 메시지를 삭제할 수 있는지 확인 - 발신자인 경우")
    void canUserDeleteMessage_WhenSender_ShouldReturnTrue() {
        // When
        boolean result = messageDomainService.canUserDeleteMessage(user, message, chatRoom);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("사용자가 메시지를 삭제할 수 있는지 확인 - 채팅방 관리자인 경우")
    void canUserDeleteMessage_WhenAdmin_ShouldReturnTrue() {
        // When
        boolean result = messageDomainService.canUserDeleteMessage(adminUser, message, chatRoom);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("사용자가 메시지를 삭제할 수 있는지 확인 - 발신자도 관리자도 아닌 경우")
    void canUserDeleteMessage_WhenNotSenderNorAdmin_ShouldReturnFalse() {
        // When
        boolean result = messageDomainService.canUserDeleteMessage(otherUser, message, chatRoom);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("메시지 삭제 권한 검증 - 권한이 있는 경우 예외 없음")
    void validateDeletePermission_WhenHasPermission_ShouldNotThrow() {
        // When & Then
        assertDoesNotThrow(() -> messageDomainService.validateDeletePermission(user, message, chatRoom));
    }

    @Test
    @DisplayName("메시지 삭제 권한 검증 - 권한이 없는 경우 예외 발생")
    void validateDeletePermission_WhenNoPermission_ShouldThrow() {
        // When & Then
        assertThrows(MessageException.class, () ->
            messageDomainService.validateDeletePermission(otherUser, message, chatRoom));
    }

    @Test
    @DisplayName("새 메시지 생성 테스트")
    void createMessage_ShouldReturnValidMessage() {
        // When
        Message result = messageDomainService.createMessage("새 메시지", user, chatRoom);

        // Then
        assertNotNull(result);
        assertEquals("새 메시지", result.getContent());
        assertEquals(user, result.getSender());
        assertEquals(chatRoom, result.getChatRoom());
        assertEquals(MessageStatus.SENT, result.getStatus());
        assertNotNull(result.getTimestamp());
    }

    @Test
    @DisplayName("메시지 상태 업데이트 테스트")
    void updateMessageStatus_ShouldUpdateStatus() {
        // When
        Message result = messageDomainService.updateMessageStatus(message, MessageStatus.READ);

        // Then
        assertEquals(MessageStatus.READ, result.getStatus());
    }
}
