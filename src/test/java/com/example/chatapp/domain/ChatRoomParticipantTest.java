package com.example.chatapp.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChatRoomParticipant 엔티티 테스트")
class ChatRoomParticipantTest {

    private User user;
    private User adminUser;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        user = User.create("testuser", "encodedPassword");
        setFieldValue(user, "id", 1L);
        
        adminUser = User.create("admin", "encodedPassword");
        setFieldValue(adminUser, "id", 2L);
        
        // 채팅방 생성
        chatRoom = ChatRoom.create("Test Room", ChatRoomType.GROUP, adminUser);
        setFieldValue(chatRoom, "id", 1L);
    }

    @Nested
    @DisplayName("참여자 생성 테스트")
    class CreateParticipantTests {

        @Test
        @DisplayName("유효한 정보로 참여자를 생성할 수 있다")
        void givenValidData_whenCreate_thenParticipantCreated() {
            // When
            ChatRoomParticipant participant = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.MEMBER);

            // Then
            assertThat(participant).isNotNull();
            assertThat(participant.getUser()).isEqualTo(user);
            assertThat(participant.getChatRoom()).isEqualTo(chatRoom);
            assertThat(participant.getRole()).isEqualTo(ParticipantRole.MEMBER);
            assertThat(participant.getJoinedAt()).isNotNull();
            assertThat(participant.getJoinedAt()).isBeforeOrEqualTo(LocalDateTime.now());
            assertThat(participant.getNotificationEnabled()).isTrue();
        }

        @Test
        @DisplayName("ADMIN 역할로 참여자를 생성할 수 있다")
        void givenAdminRole_whenCreate_thenAdminParticipantCreated() {
            // When
            ChatRoomParticipant participant = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.ADMIN);

            // Then
            assertThat(participant.getRole()).isEqualTo(ParticipantRole.ADMIN);
        }

        @Test
        @DisplayName("사용자가 null인 경우 예외가 발생한다")
        void givenNullUser_whenCreate_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> ChatRoomParticipant.of(null, chatRoom, ParticipantRole.MEMBER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자는 필수입니다");
        }

        @Test
        @DisplayName("채팅방이 null인 경우 예외가 발생한다")
        void givenNullChatRoom_whenCreate_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> ChatRoomParticipant.of(user, null, ParticipantRole.MEMBER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("채팅방은 필수입니다");
        }

        @Test
        @DisplayName("역할이 null인 경우 예외가 발생한다")
        void givenNullRole_whenCreate_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> ChatRoomParticipant.of(user, chatRoom, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("참여자 역할은 필수입니다");
        }
    }

    @Nested
    @DisplayName("역할 변경 테스트")
    class ChangeRoleTests {

        private ChatRoomParticipant participant;

        @BeforeEach
        void setUp() {
            participant = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.MEMBER);
        }

        @Test
        @DisplayName("MEMBER에서 ADMIN으로 역할을 변경할 수 있다")
        void givenMember_whenChangeToAdmin_thenRoleChanged() {
            // When
            participant.changeRole(ParticipantRole.ADMIN, adminUser);

            // Then
            assertThat(participant.getRole()).isEqualTo(ParticipantRole.ADMIN);
        }

        @Test
        @DisplayName("ADMIN에서 MEMBER로 역할을 변경할 수 있다")
        void givenAdmin_whenChangeToMember_thenRoleChanged() {
            // Given
            ChatRoomParticipant adminParticipant = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.ADMIN);

            // When
            adminParticipant.changeRole(ParticipantRole.MEMBER, adminUser);

            // Then
            assertThat(adminParticipant.getRole()).isEqualTo(ParticipantRole.MEMBER);
        }

        @Test
        @DisplayName("시스템에서 자동으로 역할을 변경할 수 있다")
        void givenSystemChange_whenChangeRole_thenRoleChanged() {
            // When - requestor가 null인 경우 (시스템 자동 변경)
            participant.changeRole(ParticipantRole.ADMIN, null);

            // Then
            assertThat(participant.getRole()).isEqualTo(ParticipantRole.ADMIN);
        }

        @Test
        @DisplayName("동일한 역할로 변경하려고 하면 예외가 발생한다")
        void givenSameRole_whenChangeRole_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> participant.changeRole(ParticipantRole.MEMBER, adminUser))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 동일한 역할입니다");
        }

        @Test
        @DisplayName("새로운 역할이 null인 경우 예외가 발생한다")
        void givenNullRole_whenChangeRole_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> participant.changeRole(null, adminUser))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("새로운 역할은 필수입니다");
        }
    }

    @Nested
    @DisplayName("알림 설정 테스트")
    class NotificationSettingTests {

        private ChatRoomParticipant participant;

        @BeforeEach
        void setUp() {
            participant = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.MEMBER);
        }

        @Test
        @DisplayName("알림 설정을 토글할 수 있다")
        void whenToggleNotification_thenNotificationToggled() {
            // Initial state
            assertThat(participant.getNotificationEnabled()).isTrue();

            // First toggle
            participant.toggleNotification();
            assertThat(participant.getNotificationEnabled()).isFalse();

            // Second toggle
            participant.toggleNotification();
            assertThat(participant.getNotificationEnabled()).isTrue();
        }

        @Test
        @DisplayName("알림 설정을 직접 설정할 수 있다")
        void givenBoolean_whenSetNotification_thenNotificationSet() {
            // When - Disable
            participant.setNotificationEnabled(false);
            assertThat(participant.getNotificationEnabled()).isFalse();

            // When - Enable
            participant.setNotificationEnabled(true);
            assertThat(participant.getNotificationEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("equals 및 hashCode 테스트")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("같은 사용자와 채팅방을 가진 참여자는 동일하다고 판단한다")
        void givenSameUserAndChatRoom_whenEquals_thenReturnTrue() {
            // Given
            ChatRoomParticipant participant1 = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.MEMBER);
            ChatRoomParticipant participant2 = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.ADMIN);

            // When & Then
            assertThat(participant1).isEqualTo(participant2);
            assertThat(participant1.hashCode()).isEqualTo(participant2.hashCode());
        }

        @Test
        @DisplayName("다른 사용자를 가진 참여자는 다르다고 판단한다")
        void givenDifferentUser_whenEquals_thenReturnFalse() {
            // Given
            User otherUser = User.create("other", "password");
            setFieldValue(otherUser, "id", 3L);
            
            ChatRoomParticipant participant1 = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.MEMBER);
            ChatRoomParticipant participant2 = ChatRoomParticipant.of(otherUser, chatRoom, ParticipantRole.MEMBER);

            // When & Then
            assertThat(participant1).isNotEqualTo(participant2);
        }

        @Test
        @DisplayName("다른 채팅방을 가진 참여자는 다르다고 판단한다")
        void givenDifferentChatRoom_whenEquals_thenReturnFalse() {
            // Given
            ChatRoom otherRoom = ChatRoom.create("Other Room", ChatRoomType.GROUP, adminUser);
            setFieldValue(otherRoom, "id", 2L);
            
            ChatRoomParticipant participant1 = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.MEMBER);
            ChatRoomParticipant participant2 = ChatRoomParticipant.of(user, otherRoom, ParticipantRole.MEMBER);

            // When & Then
            assertThat(participant1).isNotEqualTo(participant2);
        }

        @Test
        @DisplayName("동일 객체는 항상 같다")
        void givenSameObject_whenEquals_thenReturnTrue() {
            // Given
            ChatRoomParticipant participant = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.MEMBER);

            // When & Then
            assertThat(participant).isEqualTo(participant);
        }

        @Test
        @DisplayName("null과 비교하면 false를 반환한다")
        void givenNull_whenEquals_thenReturnFalse() {
            // Given
            ChatRoomParticipant participant = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.MEMBER);

            // When & Then
            assertThat(participant).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입의 객체와 비교하면 false를 반환한다")
        void givenDifferentType_whenEquals_thenReturnFalse() {
            // Given
            ChatRoomParticipant participant = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.MEMBER);
            String notParticipant = "not a participant";

            // When & Then
            assertThat(participant).isNotEqualTo(notParticipant);
        }
    }

    @Nested
    @DisplayName("연관관계 편의 메서드 테스트")
    class RelationshipTests {

        @Test
        @DisplayName("채팅방을 설정할 수 있다")
        void givenChatRoom_whenSetChatRoom_thenChatRoomSet() {
            // Given
            ChatRoomParticipant participant = ChatRoomParticipant.of(user, chatRoom, ParticipantRole.MEMBER);
            ChatRoom newChatRoom = ChatRoom.create("New Room", ChatRoomType.GROUP, adminUser);
            setFieldValue(newChatRoom, "id", 2L);

            // When
            participant.setChatRoom(newChatRoom);

            // Then
            assertThat(participant.getChatRoom()).isEqualTo(newChatRoom);
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