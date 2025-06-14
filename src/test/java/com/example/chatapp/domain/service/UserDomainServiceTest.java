package com.example.chatapp.domain.service;

import com.example.chatapp.domain.User;
import com.example.chatapp.domain.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserDomainServiceTest {

    private UserDomainService userDomainService;
    
    @BeforeEach
    void setUp() {
        userDomainService = new UserDomainService();
    }
    
    @Test
    @DisplayName("올바른 현재 비밀번호로 비밀번호 변경 성공")
    void changePassword_withCorrectCurrentPassword_shouldSucceed() {
        // Given
        String username = "testuser";
        String currentPassword = "encodedPassword123";
        String newPassword = "newEncodedPassword456";
        User user = User.create(username, currentPassword);
        
        // When
        userDomainService.changePassword(user, currentPassword, newPassword);
        
        // Then
        assertThat(user.isPasswordMatch(newPassword)).isTrue();
        assertThat(user.isPasswordMatch(currentPassword)).isFalse();
    }
    
    @Test
    @DisplayName("잘못된 현재 비밀번호로 비밀번호 변경 시 예외 발생")
    void changePassword_withIncorrectCurrentPassword_shouldThrowException() {
        // Given
        String username = "testuser";
        String currentPassword = "encodedPassword123";
        String wrongPassword = "wrongPassword";
        String newPassword = "newEncodedPassword456";
        User user = User.create(username, currentPassword);
        
        // When & Then
        assertThatThrownBy(() -> userDomainService.changePassword(user, wrongPassword, newPassword))
                .isInstanceOf(DomainException.class)
                .hasMessage("현재 비밀번호가 일치하지 않습니다");
    }
    
    @Test
    @DisplayName("현재 비밀번호와 동일한 새 비밀번호로 변경 시 예외 발생")
    void changePassword_withSamePassword_shouldThrowException() {
        // Given
        String username = "testuser";
        String currentPassword = "encodedPassword123";
        User user = User.create(username, currentPassword);
        
        // When & Then
        assertThatThrownBy(() -> userDomainService.changePassword(user, currentPassword, currentPassword))
                .isInstanceOf(DomainException.class)
                .hasMessage("새 비밀번호는 현재 비밀번호와 달라야 합니다");
    }
    
    @Test
    @DisplayName("새로운 사용자명으로 변경 성공")
    void changeUsername_withNewUsername_shouldSucceed() {
        // Given
        String oldUsername = "olduser";
        String newUsername = "newuser";
        User user = User.create(oldUsername, "password");
        
        // When
        userDomainService.changeUsername(user, newUsername);
        
        // Then
        assertThat(user.getUsername()).isEqualTo(newUsername);
    }
    
    @Test
    @DisplayName("현재와 동일한 사용자명으로 변경 시 예외 발생")
    void changeUsername_withSameUsername_shouldThrowException() {
        // Given
        String username = "testuser";
        User user = User.create(username, "password");
        
        // When & Then
        assertThatThrownBy(() -> userDomainService.changeUsername(user, username))
                .isInstanceOf(DomainException.class)
                .hasMessage("새 사용자명은 현재 사용자명과 달라야 합니다");
    }
}