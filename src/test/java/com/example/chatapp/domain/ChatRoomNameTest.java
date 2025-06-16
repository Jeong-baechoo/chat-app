package com.example.chatapp.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChatRoomName 값 객체 테스트")
class ChatRoomNameTest {

    @Nested
    @DisplayName("채팅방명 생성 테스트")
    class CreateChatRoomNameTests {

        @Test
        @DisplayName("유효한 이름으로 채팅방명을 생성할 수 있다")
        void givenValidName_whenCreate_thenChatRoomNameCreated() {
            // Given
            String validName = "General Chat Room";

            // When
            ChatRoomName chatRoomName = ChatRoomName.of(validName);

            // Then
            assertThat(chatRoomName).isNotNull();
            assertThat(chatRoomName.getValue()).isEqualTo(validName);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t", "\n"})
        @DisplayName("이름이 null이거나 빈 문자열인 경우 예외가 발생한다")
        void givenInvalidName_whenCreate_thenThrowException(String invalidName) {
            // When & Then
            assertThatThrownBy(() -> ChatRoomName.of(invalidName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("채팅방명은 필수입니다");
        }

        @Test
        @DisplayName("이름이 2자 미만인 경우 예외가 발생한다")
        void givenShortName_whenCreate_thenThrowException() {
            // Given
            String shortName = "A";

            // When & Then
            assertThatThrownBy(() -> ChatRoomName.of(shortName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("채팅방명은 최소 2자 이상이어야 합니다");
        }

        @Test
        @DisplayName("공백만으로 이루어진 2자 이상 문자열도 실제 길이가 2자 미만이면 예외가 발생한다")
        void givenSpacePaddedShortName_whenCreate_thenThrowException() {
            // Given
            String spacePaddedName = " A ";

            // When & Then
            assertThatThrownBy(() -> ChatRoomName.of(spacePaddedName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("채팅방명은 최소 2자 이상이어야 합니다");
        }

        @Test
        @DisplayName("이름이 100자를 초과하는 경우 예외가 발생한다")
        void givenLongName_whenCreate_thenThrowException() {
            // Given
            String longName = "a".repeat(101);

            // When & Then
            assertThatThrownBy(() -> ChatRoomName.of(longName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("채팅방명은 100자를 초과할 수 없습니다");
        }

        @ParameterizedTest
        @ValueSource(strings = {"<script>", "Room&Name", "Room\"Name", "Room'Name", "Room>Name"})
        @DisplayName("허용되지 않은 특수문자가 포함된 경우 예외가 발생한다")
        void givenNameWithInvalidCharacters_whenCreate_thenThrowException(String invalidName) {
            // When & Then
            assertThatThrownBy(() -> ChatRoomName.of(invalidName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("채팅방명에 사용할 수 없는 문자가 포함되어 있습니다");
        }

        @ParameterizedTest
        @ValueSource(strings = {"채팅방", "Chat Room #1", "방 1234", "Team-A", "Room_1", "!@#$%", "방(1)"})
        @DisplayName("허용된 문자로 구성된 이름은 생성할 수 있다")
        void givenNameWithAllowedCharacters_whenCreate_thenSuccess(String validName) {
            // When
            ChatRoomName chatRoomName = ChatRoomName.of(validName);

            // Then
            assertThat(chatRoomName.getValue()).isEqualTo(validName);
        }
    }

    @Nested
    @DisplayName("equals 및 hashCode 테스트")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("같은 값을 가진 채팅방명은 동일하다고 판단한다")
        void givenSameValue_whenEquals_thenReturnTrue() {
            // Given
            ChatRoomName name1 = ChatRoomName.of("Test Room");
            ChatRoomName name2 = ChatRoomName.of("Test Room");

            // When & Then
            assertThat(name1).isEqualTo(name2);
            assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
        }

        @Test
        @DisplayName("다른 값을 가진 채팅방명은 다르다고 판단한다")
        void givenDifferentValue_whenEquals_thenReturnFalse() {
            // Given
            ChatRoomName name1 = ChatRoomName.of("Room 1");
            ChatRoomName name2 = ChatRoomName.of("Room 2");

            // When & Then
            assertThat(name1).isNotEqualTo(name2);
        }

        @Test
        @DisplayName("동일 객체는 항상 같다")
        void givenSameObject_whenEquals_thenReturnTrue() {
            // Given
            ChatRoomName name = ChatRoomName.of("Test Room");

            // When & Then
            assertThat(name).isEqualTo(name);
        }

        @Test
        @DisplayName("null과 비교하면 false를 반환한다")
        void givenNull_whenEquals_thenReturnFalse() {
            // Given
            ChatRoomName name = ChatRoomName.of("Test Room");

            // When & Then
            assertThat(name).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입의 객체와 비교하면 false를 반환한다")
        void givenDifferentType_whenEquals_thenReturnFalse() {
            // Given
            ChatRoomName name = ChatRoomName.of("Test Room");
            String notChatRoomName = "Test Room";

            // When & Then
            assertThat(name).isNotEqualTo(notChatRoomName);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString은 내부 값을 반환한다")
        void whenToString_thenReturnValue() {
            // Given
            String roomName = "My Chat Room";
            ChatRoomName chatRoomName = ChatRoomName.of(roomName);

            // When
            String result = chatRoomName.toString();

            // Then
            assertThat(result).isEqualTo(roomName);
        }
    }
}