package com.example.chatapp.service.unit.user;

import com.example.chatapp.domain.User;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.mapper.UserMapper;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
        userService = new UserServiceImpl(userRepository, userMapper, null);
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
