package com.example.chatapp.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Message 엔티티 테스트")
class MessageTest {

    private static final String VALID_CONTENT = "Hello, this is a test message!";
    
    private User sender;
    private User otherUser;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        // 발신자 생성
        sender = User.create("sender", "encodedPassword");
        setFieldValue(sender, "id", 1L);
        
        // 다른 사용자 생성
        otherUser = User.create("other", "encodedPassword");
        setFieldValue(otherUser, "id", 2L);
        
        // 채팅방 생성 (발신자가 참여자로 포함됨)
        chatRoom = ChatRoom.create("Test Room", ChatRoomType.GROUP, sender);
        setFieldValue(chatRoom, "id", 1L);
    }

    @Nested
    @DisplayName("메시지 생성 테스트")
    class CreateMessageTests {

        @Test
        @DisplayName("유효한 정보로 메시지를 생성할 수 있다")
        void givenValidData_whenCreate_thenMessageCreated() {
            // When
            Message message = Message.create(VALID_CONTENT, sender, chatRoom);

            // Then
            assertThat(message).isNotNull();
            assertThat(message.getContent()).isEqualTo(VALID_CONTENT);
            assertThat(message.getSender()).isEqualTo(sender);
            assertThat(message.getChatRoom()).isEqualTo(chatRoom);
            assertThat(message.getStatus()).isEqualTo(MessageStatus.SENT);
            assertThat(message.getTimestamp()).isNotNull();
            assertThat(message.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t", "\n"})
        @DisplayName("내용이 null이거나 빈 문자열인 경우 예외가 발생한다")
        void givenInvalidContent_whenCreate_thenThrowException(String invalidContent) {
            // When & Then
            assertThatThrownBy(() -> Message.create(invalidContent, sender, chatRoom))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("메시지 내용은 필수입니다");
        }

        @Test
        @DisplayName("너무 긴 메시지를 생성하면 예외가 발생한다")
        void givenTooLongContent_whenCreate_thenThrowException() {
            // Given
            String longContent = "a".repeat(1001);

            // When & Then
            assertThatThrownBy(() -> Message.create(longContent, sender, chatRoom))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("메시지는 1000자를 초과할 수 없습니다");
        }

        @Test
        @DisplayName("발신자가 null인 경우 예외가 발생한다")
        void givenNullSender_whenCreate_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> Message.create(VALID_CONTENT, null, chatRoom))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("메시지 발신자는 필수입니다");
        }

        @Test
        @DisplayName("채팅방이 null인 경우 예외가 발생한다")
        void givenNullChatRoom_whenCreate_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> Message.create(VALID_CONTENT, sender, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("채팅방은 필수입니다");
        }

        @Test
        @DisplayName("발신자가 채팅방 참여자가 아닌 경우 예외가 발생한다")
        void givenNonParticipantSender_whenCreate_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> Message.create(VALID_CONTENT, otherUser, chatRoom))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("채팅방 참여자만 메시지를 보낼 수 있습니다");
        }
    }

    @Nested
    @DisplayName("메시지 상태 업데이트 테스트")
    class UpdateStatusTests {

        private Message message;

        @BeforeEach
        void setUp() {
            message = Message.create(VALID_CONTENT, sender, chatRoom);
        }

        @Test
        @DisplayName("메시지 상태를 READ로 변경할 수 있다")
        void givenSentMessage_whenUpdateStatus_thenStatusChanged() {
            // When
            message.updateStatus(MessageStatus.READ);

            // Then
            assertThat(message.getStatus()).isEqualTo(MessageStatus.READ);
        }

        @Test
        @DisplayName("null 상태로 변경하면 예외가 발생한다")
        void givenNullStatus_whenUpdateStatus_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> message.updateStatus(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("메시지 상태는 필수입니다");
        }

        @Test
        @DisplayName("이미 읽은 메시지를 다시 SENT로 변경할 수 있다")
        void givenReadMessage_whenUpdateToSent_thenStatusChanged() {
            // Given
            message.updateStatus(MessageStatus.READ);

            // When
            message.updateStatus(MessageStatus.SENT);

            // Then
            assertThat(message.getStatus()).isEqualTo(MessageStatus.SENT);
        }
    }

    @Nested
    @DisplayName("메시지 속성 조회 테스트")
    class MessagePropertyTests {

        private Message message;

        @BeforeEach
        void setUp() {
            message = Message.create(VALID_CONTENT, sender, chatRoom);
        }

        @Test
        @DisplayName("메시지 상태를 확인할 수 있다")
        void whenCheckMessageStatus_thenReturnCorrectResult() {
            // Initially
            assertThat(message.getStatus()).isEqualTo(MessageStatus.SENT);

            // After update
            message.updateStatus(MessageStatus.READ);
            assertThat(message.getStatus()).isEqualTo(MessageStatus.READ);
        }

        @Test
        @DisplayName("메시지 타임스탬프를 확인할 수 있다")
        void whenGetTimestamp_thenReturnNotNull() {
            // When & Then
            assertThat(message.getTimestamp()).isNotNull();
            assertThat(message.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("메시지 내용을 확인할 수 있다")
        void whenGetContent_thenReturnCorrectContent() {
            // When & Then
            assertThat(message.getContent()).isEqualTo(VALID_CONTENT);
            assertThat(message.getContent()).contains("test");
        }
    }

    @Nested
    @DisplayName("메시지 equals와 hashCode 테스트")
    class MessageEqualsAndHashCodeTests {

        @Test
        @DisplayName("같은 ID를 가진 메시지는 같다")
        void givenSameId_whenEquals_thenTrue() {
            // Given
            Message message1 = Message.create(VALID_CONTENT, sender, chatRoom);
            Message message2 = Message.create("Other content", sender, chatRoom);
            setFieldValue(message1, "id", 1L);
            setFieldValue(message2, "id", 1L);

            // When & Then
            assertThat(message1).isEqualTo(message2);
        }

        @Test
        @DisplayName("ID가 없는 새 메시지들은 다르다")
        void givenNoId_whenEquals_thenFalse() {
            // Given
            Message message1 = Message.create(VALID_CONTENT, sender, chatRoom);
            Message message2 = Message.create(VALID_CONTENT, sender, chatRoom);

            // When & Then
            assertThat(message1).isNotEqualTo(message2);
        }

        @Test
        @DisplayName("hashCode는 클래스 기반으로 일관되게 생성된다")
        void whenHashCode_thenConsistent() {
            // Given
            Message message1 = Message.create(VALID_CONTENT, sender, chatRoom);
            Message message2 = Message.create("Other content", sender, chatRoom);

            // When & Then
            assertThat(message1.hashCode()).isEqualTo(message2.hashCode());
            assertThat(message1.hashCode()).isEqualTo(Message.class.hashCode());
        }
    }

    // 테스트용 헬퍼 메서드
    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}