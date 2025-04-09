package com.example.chatapp.service.unit.user;

import com.example.chatapp.domain.User;
import com.example.chatapp.domain.UserStatus;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.mapper.UserMapper;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long NONEXISTENT_ID = 999L;
    private static final String TEST_USERNAME = "testuser";
    private static final String ERROR_USER_NOT_FOUND = "사용자를 찾을 수 없습니다";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper);
    }

    @Test
    @DisplayName("givenUserId_whenUpdateUserStatus_thenStatusIsUpdated")
    void givenUserId_whenUpdateUserStatus_thenStatusIsUpdated() {
        // Given
        User testUser = User.builder()
                .id(USER_ID)
                .username(TEST_USERNAME)
                .build();

        User updatedUser = User.builder()
                .id(USER_ID)
                .username(TEST_USERNAME)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(USER_ID)
                .username(TEST_USERNAME)
                .status(UserStatus.ONLINE)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenReturn(userResponse);

        // When
        UserResponse result = userService.updateUserStatus(USER_ID, UserStatus.ONLINE);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ONLINE);

        // 사용자 조회 및 저장 검증
        verify(userRepository).findById(USER_ID);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        verify(userMapper).toResponse(updatedUser);
    }

    @Test
    @DisplayName("givenNonExistentUserId_whenUpdateUserStatus_thenThrowException")
    void givenNonExistentUserId_whenUpdateUserStatus_thenThrowException() {
        // Given
        when(userRepository.findById(NONEXISTENT_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUserStatus(NONEXISTENT_ID, UserStatus.OFFLINE))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository).findById(NONEXISTENT_ID);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("givenUserId_whenDeleteUser_thenUserIsDeleted")
    void givenUserId_whenDeleteUser_thenUserIsDeleted() {
        // Given
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        // When
        userService.deleteUser(USER_ID);

        // Then
        verify(userRepository).existsById(USER_ID);
        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("givenNonExistentUserId_whenDeleteUser_thenThrowException")
    void givenNonExistentUserId_whenDeleteUser_thenThrowException() {
        // Given
        when(userRepository.existsById(NONEXISTENT_ID)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(NONEXISTENT_ID))
                .isInstanceOf(RuntimeException.class);

        verify(userRepository).existsById(NONEXISTENT_ID);
        verify(userRepository, never()).deleteById(anyLong());
    }
}
