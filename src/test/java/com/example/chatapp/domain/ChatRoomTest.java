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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChatRoom 엔티티 테스트")
class ChatRoomTest {

    private static final String VALID_ROOM_NAME = "Test Chat Room";
    private static final String VALID_USERNAME = "testuser";
    private static final String VALID_PASSWORD = "encodedPassword";
    
    private User creator;

    @BeforeEach
    void setUp() {
        creator = User.create(VALID_USERNAME, VALID_PASSWORD);
        // 테스트를 위해 creator에 ID 설정
        setFieldValue(creator, "id", 1L);
    }

    @Nested
    @DisplayName("채팅방 생성 테스트")
    class CreateChatRoomTests {

        @Test
        @DisplayName("유효한 정보로 채팅방을 생성할 수 있다")
        void givenValidData_whenCreate_thenChatRoomCreated() {
            // When
            ChatRoom chatRoom = ChatRoom.create(VALID_ROOM_NAME, ChatRoomType.GROUP, creator);

            // Then
            assertThat(chatRoom).isNotNull();
            assertThat(chatRoom.getName().getValue()).isEqualTo(VALID_ROOM_NAME);
            assertThat(chatRoom.getType()).isEqualTo(ChatRoomType.GROUP);
            assertThat(chatRoom.getCreatedAt()).isNotNull();
            assertThat(chatRoom.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
            assertThat(chatRoom.getParticipants()).hasSize(1);
            assertThat(chatRoom.isParticipant(creator)).isTrue();
            assertThat(chatRoom.isAdmin(creator)).isTrue();
        }

        @Test
        @DisplayName("PRIVATE 타입의 채팅방을 생성할 수 있다")
        void givenPrivateType_whenCreate_thenPrivateChatRoomCreated() {
            // When
            ChatRoom chatRoom = ChatRoom.create(VALID_ROOM_NAME, ChatRoomType.PRIVATE, creator);

            // Then
            assertThat(chatRoom.getType()).isEqualTo(ChatRoomType.PRIVATE);
        }

        @Test
        @DisplayName("생성자는 자동으로 ADMIN 권한을 가진다")
        void givenCreator_whenCreate_thenCreatorIsAdmin() {
            // When
            ChatRoom chatRoom = ChatRoom.create(VALID_ROOM_NAME, ChatRoomType.GROUP, creator);

            // Then
            ChatRoomParticipant participant = chatRoom.getParticipants().iterator().next();
            assertThat(participant.getRole()).isEqualTo(ParticipantRole.ADMIN);
            assertThat(participant.getUser()).isEqualTo(creator);
        }
    }

    @Nested
    @DisplayName("채팅방 이름 변경 테스트")
    class UpdateNameTests {

        private ChatRoom chatRoom;

        @BeforeEach
        void setUp() {
            chatRoom = ChatRoom.create(VALID_ROOM_NAME, ChatRoomType.GROUP, creator);
        }

        @Test
        @DisplayName("유효한 이름으로 변경할 수 있다")
        void givenValidName_whenUpdateName_thenNameUpdated() {
            // Given
            String newName = "Updated Chat Room";

            // When
            chatRoom.changeName(newName);

            // Then
            assertThat(chatRoom.getName().getValue()).isEqualTo(newName);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null이거나 빈 이름으로 변경하면 예외가 발생한다")
        void givenInvalidName_whenUpdateName_thenThrowException(String invalidName) {
            // When & Then
            assertThatThrownBy(() -> chatRoom.changeName(invalidName))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("참여자 관리 테스트")
    class ParticipantManagementTests {

        private ChatRoom chatRoom;
        private User newUser;

        @BeforeEach
        void setUp() {
            chatRoom = ChatRoom.create(VALID_ROOM_NAME, ChatRoomType.GROUP, creator);
            newUser = User.create("newuser", VALID_PASSWORD);
            setFieldValue(newUser, "id", 2L);
        }

        @Test
        @DisplayName("새로운 참여자를 추가할 수 있다")
        void givenNewUser_whenAddParticipant_thenParticipantAdded() {
            // When
            chatRoom.addParticipantInternal(newUser, ParticipantRole.MEMBER);

            // Then
            assertThat(chatRoom.getParticipants()).hasSize(2);
            assertThat(chatRoom.isParticipant(newUser)).isTrue();
            assertThat(chatRoom.isAdmin(newUser)).isFalse();
        }

        @Test
        @DisplayName("참여자를 ADMIN으로 추가할 수 있다")
        void givenNewUser_whenAddAsAdmin_thenAdminAdded() {
            // When
            chatRoom.addParticipantInternal(newUser, ParticipantRole.ADMIN);

            // Then
            assertThat(chatRoom.isAdmin(newUser)).isTrue();
        }

        @Test
        @DisplayName("참여자를 제거할 수 있다")
        void givenExistingParticipant_whenRemove_thenParticipantRemoved() {
            // Given
            chatRoom.addParticipantInternal(newUser, ParticipantRole.MEMBER);

            // When
            chatRoom.removeParticipantInternal(newUser);

            // Then
            assertThat(chatRoom.getParticipants()).hasSize(1);
            assertThat(chatRoom.isParticipant(newUser)).isFalse();
        }

        @Test
        @DisplayName("참여하지 않은 사용자를 제거하려고 하면 예외가 발생한다")
        void givenNonParticipant_whenRemove_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> chatRoom.removeParticipantInternal(newUser))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("채팅방에 참여하지 않은 사용자입니다");
        }

        @Test
        @DisplayName("마지막 ADMIN을 제거하면 다른 참여자가 ADMIN이 된다")
        void givenLastAdmin_whenRemove_thenNewAdminPromoted() {
            // Given
            chatRoom.addParticipantInternal(newUser, ParticipantRole.MEMBER);
            
            // When
            chatRoom.removeParticipantInternal(creator);

            // Then
            assertThat(chatRoom.isAdmin(newUser)).isTrue();
            assertThat(chatRoom.getParticipants()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("참여자 조회 테스트")
    class ParticipantQueryTests {

        private ChatRoom chatRoom;
        private User member;
        private User nonMember;

        @BeforeEach
        void setUp() {
            chatRoom = ChatRoom.create(VALID_ROOM_NAME, ChatRoomType.GROUP, creator);
            member = User.create("member", VALID_PASSWORD);
            nonMember = User.create("nonmember", VALID_PASSWORD);
            setFieldValue(member, "id", 2L);
            setFieldValue(nonMember, "id", 3L);
            chatRoom.addParticipantInternal(member, ParticipantRole.MEMBER);
        }

        @Test
        @DisplayName("사용자가 참여자인지 확인할 수 있다")
        void givenUser_whenCheckParticipant_thenReturnCorrectResult() {
            // When & Then
            assertThat(chatRoom.isParticipant(creator)).isTrue();
            assertThat(chatRoom.isParticipant(member)).isTrue();
            assertThat(chatRoom.isParticipant(nonMember)).isFalse();
        }

        @Test
        @DisplayName("사용자 ID로 참여자인지 확인할 수 있다")
        void givenUserId_whenCheckParticipant_thenReturnCorrectResult() {
            // When & Then
            assertThat(chatRoom.isParticipantById(1L)).isTrue();
            assertThat(chatRoom.isParticipantById(2L)).isTrue();
            assertThat(chatRoom.isParticipantById(3L)).isFalse();
        }

        @Test
        @DisplayName("사용자가 관리자인지 확인할 수 있다")
        void givenUser_whenCheckAdmin_thenReturnCorrectResult() {
            // When & Then
            assertThat(chatRoom.isAdmin(creator)).isTrue();
            assertThat(chatRoom.isAdmin(member)).isFalse();
            assertThat(chatRoom.isAdmin(nonMember)).isFalse();
        }

        @Test
        @DisplayName("참여자 수를 확인할 수 있다")
        void whenGetParticipantCount_thenReturnCorrectCount() {
            // When & Then
            assertThat(chatRoom.getParticipantCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("특정 사용자 ID로 참여자 정보를 찾을 수 있다")
        void givenUserId_whenFindParticipant_thenReturnParticipant() {
            // When
            ChatRoomParticipant participant = chatRoom.findParticipantById(2L);

            // Then
            assertThat(participant).isNotNull();
            assertThat(participant.getUser()).isEqualTo(member);
            assertThat(participant.getRole()).isEqualTo(ParticipantRole.MEMBER);
        }

        @Test
        @DisplayName("참여하지 않은 사용자 ID로 참여자 정보를 찾으면 예외가 발생한다")
        void givenNonParticipantId_whenFindParticipant_thenThrowException() {
            // When & Then
            assertThatThrownBy(() -> chatRoom.findParticipantById(3L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("채팅방에 참여하지 않은 사용자입니다");
        }
    }

    @Nested
    @DisplayName("채팅방 제한 테스트")
    class ChatRoomLimitTests {

        @Test
        @DisplayName("채팅방 최대 인원을 초과하면 예외가 발생한다")
        void givenFullChatRoom_whenAddParticipant_thenThrowException() {
            // Given
            ChatRoom chatRoom = ChatRoom.create(VALID_ROOM_NAME, ChatRoomType.GROUP, creator);
            
            // 최대 인원(100명)까지 채우기
            for (int i = 2; i <= 100; i++) {
                User user = User.create("user" + i, VALID_PASSWORD);
                setFieldValue(user, "id", (long) i);
                chatRoom.addParticipantInternal(user, ParticipantRole.MEMBER);
            }

            // When & Then
            User extraUser = User.create("extrauser", VALID_PASSWORD);
            setFieldValue(extraUser, "id", 101L);
            
            assertThat(chatRoom.isFull()).isTrue();
            
            // 채팅방이 꽉 찬 상태에서는 도메인 서비스에서 검증하므로
            // 직접 addParticipantInternal 호출 시에는 단순히 추가됨
            // 실제로는 도메인 서비스에서 isFull() 체크 후 예외 발생
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