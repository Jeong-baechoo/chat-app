package com.example.chatapp.domain.service;

import com.example.chatapp.domain.*;
import com.example.chatapp.exception.MessageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageDomainServiceTest {

    @InjectMocks
    private MessageDomainService messageDomainService;

    // 실제 객체들은 Mock 대신 실제 객체로 사용 (도메인 서비스 테스트이기 때문에)
    private User user;
    private User otherUser;
    private User adminUser;
    private ChatRoom chatRoom;
    private Message message;
    private ChatRoomParticipant participant;
    private ChatRoomParticipant adminParticipant;

    @BeforeEach
    void setUp() {

        // Given: 사용자 설정
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

        // Given: 채팅방 참여자 설정
        participant = new ChatRoomParticipant();
        participant.setUser(user);
        participant.setRole(ParticipantRole.MEMBER);

        adminParticipant = new ChatRoomParticipant();
        adminParticipant.setUser(adminUser);
        adminParticipant.setRole(ParticipantRole.ADMIN);

        // Given: 채팅방 설정
        chatRoom = new ChatRoom();
        chatRoom.setId(1L);
        chatRoom.setName("Test Room");

        // Given: 채팅방 참여자 설정
        Set<ChatRoomParticipant> participants = new HashSet<>();
        participants.add(participant);
        participants.add(adminParticipant);
        chatRoom.setParticipants(participants);

        // Given: 메시지 설정
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
    void givenUserIsParticipant_whenCheckCanSendMessage_thenReturnTrue() {
        // Given: 참여자인 사용자 (setUp에서 설정됨)

        // When: 메시지 전송 권한 확인
        boolean result = messageDomainService.canUserSendMessage(user, chatRoom);

        // Then: 참여자이므로 true 반환
        assertTrue(result, "채팅방 참여자는 메시지를 전송할 수 있어야 합니다");
    }

    @Test
    @DisplayName("사용자가 메시지를 전송할 수 있는지 확인 - 참여자가 아닌 경우")
    void givenUserIsNotParticipant_whenCheckCanSendMessage_thenReturnFalse() {
        // Given: 참여자가 아닌 사용자 (setUp에서 설정됨)

        // When: 메시지 전송 권한 확인
        boolean result = messageDomainService.canUserSendMessage(otherUser, chatRoom);

        // Then: 참여자가 아니므로 false 반환
        assertFalse(result, "채팅방 참여자가 아닌 사용자는 메시지를 전송할 수 없어야 합니다");
    }

    @Test
    @DisplayName("사용자가 메시지를 업데이트할 수 있는지 확인 - 발신자인 경우")
    void givenUserIsSender_whenCheckCanUpdateMessage_thenReturnTrue() {
        // Given: 메시지 발신자 (setUp에서 설정됨)

        // When: 메시지 업데이트 권한 확인
        boolean result = messageDomainService.canUserUpdateMessage(user, message);

        // Then: 발신자이므로 true 반환
        assertTrue(result, "메시지 발신자는 메시지를 업데이트할 수 있어야 합니다");
    }

    @Test
    @DisplayName("사용자가 메시지를 업데이트할 수 있는지 확인 - 발신자가 아닌 경우")
    void givenUserIsNotSender_whenCheckCanUpdateMessage_thenReturnFalse() {
        // Given: 메시지 발신자가 아닌 사용자 (setUp에서 설정됨)

        // When: 메시지 업데이트 권한 확인
        boolean result = messageDomainService.canUserUpdateMessage(otherUser, message);

        // Then: 발신자가 아니므로 false 반환
        assertFalse(result, "메시지 발신자가 아닌 사용자는 메시지를 업데이트할 수 없어야 합니다");
    }

    @Test
    @DisplayName("사용자가 메시지를 삭제할 수 있는지 확인 - 발신자인 경우")
    void givenUserIsSender_whenCheckCanDeleteMessage_thenReturnTrue() {
        // Given: 메시지 발신자 (setUp에서 설정됨)

        // When: 메시지 삭제 권한 확인
        boolean result = messageDomainService.canUserDeleteMessage(user, message, chatRoom);

        // Then: 발신자이므로 true 반환
        assertTrue(result, "메시지 발신자는 메시지를 삭제할 수 있어야 합니다");
    }

    @Test
    @DisplayName("사용자가 메시지를 삭제할 수 있는지 확인 - 채팅방 관리자인 경우")
    void givenUserIsAdmin_whenCheckCanDeleteMessage_thenReturnTrue() {
        // Given: 채팅방 관리자 (setUp에서 설정됨)

        // When: 메시지 삭제 권한 확인
        boolean result = messageDomainService.canUserDeleteMessage(adminUser, message, chatRoom);

        // Then: 관리자이므로 true 반환
        assertTrue(result, "채팅방 관리자는 메시지를 삭제할 수 있어야 합니다");
    }

    @Test
    @DisplayName("사용자가 메시지를 삭제할 수 있는지 확인 - 발신자도 관리자도 아닌 경우")
    void givenUserIsNotSenderNorAdmin_whenCheckCanDeleteMessage_thenReturnFalse() {
        // Given: 발신자도 관리자도 아닌 사용자 (setUp에서 설정됨)

        // When: 메시지 삭제 권한 확인
        boolean result = messageDomainService.canUserDeleteMessage(otherUser, message, chatRoom);

        // Then: 발신자도 관리자도 아니므로 false 반환
        assertFalse(result, "발신자도 관리자도 아닌 사용자는 메시지를 삭제할 수 없어야 합니다");
    }

    @Test
    @DisplayName("메시지 삭제 권한 검증 - 권한이 있는 경우 예외 없음")
    void givenUserHasPermission_whenValidateDeletePermission_thenNoExceptionThrown() {
        // Given: 메시지 발신자 (setUp에서 설정됨)

        // When & Then: 권한 검증 시 예외가 발생하지 않음
        assertDoesNotThrow(() -> messageDomainService.validateDeletePermission(user, message, chatRoom),
                "메시지 발신자는 삭제 시 예외가 발생하지 않아야 합니다");
    }

    @Test
    @DisplayName("메시지 삭제 권한 검증 - 권한이 없는 경우 예외 발생")
    void givenUserHasNoPermission_whenValidateDeletePermission_thenExceptionThrown() {
        // Given: 권한이 없는 사용자 (setUp에서 설정됨)

        // When & Then: 권한 검증 시 예외 발생
        assertThrows(MessageException.class,
                () -> messageDomainService.validateDeletePermission(otherUser, message, chatRoom),
                "권한이 없는 사용자가 메시지 삭제 시 예외가 발생해야 합니다");
    }

    @Test
    @DisplayName("새 메시지 생성 테스트")
    void givenMessageData_whenCreateMessage_thenReturnValidMessage() {
        // Given: 메시지 내용, 발신자, 채팅방
        String content = "새 메시지";

        // When: 메시지 생성
        Message result = messageDomainService.createMessage(content, user, chatRoom);

        // Then: 생성된 메시지 검증
        assertNotNull(result, "생성된 메시지는 null이 아니어야 합니다");
        assertEquals(content, result.getContent(), "메시지 내용이 일치해야 합니다");
        assertEquals(user, result.getSender(), "발신자가 일치해야 합니다");
        assertEquals(chatRoom, result.getChatRoom(), "채팅방이 일치해야 합니다");
        assertEquals(MessageStatus.SENT, result.getStatus(), "초기 상태는 SENT여야 합니다");
        assertNotNull(result.getTimestamp(), "타임스탬프가 설정되어야 합니다");
    }

    @Test
    @DisplayName("메시지 상태 업데이트 테스트")
    void givenMessageAndStatus_whenUpdateMessageStatus_thenStatusUpdated() {
        // Given: 메시지와 새 상태
        MessageStatus newStatus = MessageStatus.READ;

        // When: 메시지 상태 업데이트
        Message result = messageDomainService.updateMessageStatus(message, newStatus);

        // Then: 업데이트된 상태 검증
        assertEquals(newStatus, result.getStatus(), "메시지 상태가 올바르게 업데이트되어야 합니다");
    }
}
