package com.example.chatapp.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User 엔티티 테스트")
class UserTest {

    private static final String VALID_USERNAME = "testuser123";
    private static final String VALID_PASSWORD = "encodedPassword123";

    @Nested
    @DisplayName("사용자 생성 테스트")
    class CreateUserTests {

        @Test
        @DisplayName("유효한 정보로 사용자를 생성할 수 있다")
        void givenValidData_whenCreate_thenUserCreated() {
            // When
            User user = User.create(VALID_USERNAME, VALID_PASSWORD);

            // Then
            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo(VALID_USERNAME);
            assertThat(user.getPassword()).isEqualTo(VALID_PASSWORD);
            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t", "\n"})
        @DisplayName("사용자명이 null이거나 빈 문자열인 경우 예외가 발생한다")
        void givenInvalidUsername_whenCreate_thenThrowException(String invalidUsername) {
            // When & Then
            assertThatThrownBy(() -> User.create(invalidUsername, VALID_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자명은 필수입니다");
        }

        @Test
        @DisplayName("사용자명이 3자 미만인 경우 예외가 발생한다")
        void givenShortUsername_whenCreate_thenThrowException() {
            // Given
            String shortUsername = "ab";

            // When & Then
            assertThatThrownBy(() -> User.create(shortUsername, VALID_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자명은 3-50자 사이여야 합니다");
        }

        @Test
        @DisplayName("사용자명이 50자 초과인 경우 예외가 발생한다")
        void givenLongUsername_whenCreate_thenThrowException() {
            // Given
            String longUsername = "a".repeat(51);

            // When & Then
            assertThatThrownBy(() -> User.create(longUsername, VALID_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자명은 3-50자 사이여야 합니다");
        }

        @ParameterizedTest
        @ValueSource(strings = {"user@name", "user name", "user-name", "유저네임", "user!name"})
        @DisplayName("사용자명에 허용되지 않은 문자가 포함된 경우 예외가 발생한다")
        void givenInvalidCharacters_whenCreate_thenThrowException(String invalidUsername) {
            // When & Then
            assertThatThrownBy(() -> User.create(invalidUsername, VALID_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자명은 영문, 숫자, 밑줄(_)만 사용할 수 있습니다");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t", "\n"})
        @DisplayName("비밀번호가 null이거나 빈 문자열인 경우 예외가 발생한다")
        void givenInvalidPassword_whenCreate_thenThrowException(String invalidPassword) {
            // When & Then
            assertThatThrownBy(() -> User.create(VALID_USERNAME, invalidPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비밀번호는 필수입니다");
        }
    }

    @Nested
    @DisplayName("비밀번호 변경 테스트")
    class ChangePasswordTests {

        @Test
        @DisplayName("유효한 비밀번호로 변경할 수 있다")
        void givenValidPassword_whenChangePassword_thenPasswordChanged() {
            // Given
            User user = User.create(VALID_USERNAME, VALID_PASSWORD);
            String newPassword = "newEncodedPassword123";

            // When
            user.changePassword(newPassword);

            // Then
            assertThat(user.getPassword()).isEqualTo(newPassword);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("새 비밀번호가 null이거나 빈 문자열인 경우 예외가 발생한다")
        void givenInvalidPassword_whenChangePassword_thenThrowException(String invalidPassword) {
            // Given
            User user = User.create(VALID_USERNAME, VALID_PASSWORD);

            // When & Then
            assertThatThrownBy(() -> user.changePassword(invalidPassword))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비밀번호는 필수입니다");
        }
    }

    @Nested
    @DisplayName("비밀번호 매칭 테스트")
    class PasswordMatchTests {

        @Test
        @DisplayName("올바른 비밀번호와 매칭되면 true를 반환한다")
        void givenCorrectPassword_whenIsPasswordMatch_thenReturnTrue() {
            // Given
            User user = User.create(VALID_USERNAME, VALID_PASSWORD);

            // When
            boolean result = user.isPasswordMatch(VALID_PASSWORD);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("잘못된 비밀번호와 매칭되면 false를 반환한다")
        void givenIncorrectPassword_whenIsPasswordMatch_thenReturnFalse() {
            // Given
            User user = User.create(VALID_USERNAME, VALID_PASSWORD);
            String wrongPassword = "wrongPassword";

            // When
            boolean result = user.isPasswordMatch(wrongPassword);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("사용자명 변경 테스트")
    class ChangeUsernameTests {

        @Test
        @DisplayName("유효한 사용자명으로 변경할 수 있다")
        void givenValidUsername_whenChangeUsername_thenUsernameChanged() {
            // Given
            User user = User.create(VALID_USERNAME, VALID_PASSWORD);
            String newUsername = "newusername123";

            // When
            user.changeUsername(newUsername);

            // Then
            assertThat(user.getUsername()).isEqualTo(newUsername);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("새 사용자명이 null이거나 빈 문자열인 경우 예외가 발생한다")
        void givenInvalidUsername_whenChangeUsername_thenThrowException(String invalidUsername) {
            // Given
            User user = User.create(VALID_USERNAME, VALID_PASSWORD);

            // When & Then
            assertThatThrownBy(() -> user.changeUsername(invalidUsername))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자명은 필수입니다");
        }

        @Test
        @DisplayName("새 사용자명이 유효성 검증을 통과하지 못하면 예외가 발생한다")
        void givenInvalidFormat_whenChangeUsername_thenThrowException() {
            // Given
            User user = User.create(VALID_USERNAME, VALID_PASSWORD);
            String invalidUsername = "user@invalid";

            // When & Then
            assertThatThrownBy(() -> user.changeUsername(invalidUsername))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자명은 영문, 숫자, 밑줄(_)만 사용할 수 있습니다");
        }
    }

    @Nested
    @DisplayName("equals 및 hashCode 테스트")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("같은 username을 가진 사용자는 동일하다고 판단한다")
        void givenSameUsername_whenEquals_thenReturnTrue() {
            // Given
            User user1 = User.create(VALID_USERNAME, VALID_PASSWORD);
            User user2 = User.create(VALID_USERNAME, "differentPassword");

            // When & Then
            assertThat(user1).isEqualTo(user2);
            assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
        }

        @Test
        @DisplayName("다른 username을 가진 사용자는 다르다고 판단한다")
        void givenDifferentUsername_whenEquals_thenReturnFalse() {
            // Given
            User user1 = User.create(VALID_USERNAME, VALID_PASSWORD);
            User user2 = User.create("differentuser", VALID_PASSWORD);

            // When & Then
            assertThat(user1).isNotEqualTo(user2);
        }

        @Test
        @DisplayName("동일 객체는 항상 같다")
        void givenSameObject_whenEquals_thenReturnTrue() {
            // Given
            User user = User.create(VALID_USERNAME, VALID_PASSWORD);

            // When & Then
            assertThat(user).isEqualTo(user);
        }

        @Test
        @DisplayName("null과 비교하면 false를 반환한다")
        void givenNull_whenEquals_thenReturnFalse() {
            // Given
            User user = User.create(VALID_USERNAME, VALID_PASSWORD);

            // When & Then
            assertThat(user).isNotEqualTo(null);
        }

        @Test
        @DisplayName("다른 타입의 객체와 비교하면 false를 반환한다")
        void givenDifferentType_whenEquals_thenReturnFalse() {
            // Given
            User user = User.create(VALID_USERNAME, VALID_PASSWORD);
            String notUser = "not a user";

            // When & Then
            assertThat(user).isNotEqualTo(notUser);
        }
    }
}