package com.example.chatapp.service;

import com.example.chatapp.domain.User;
import com.example.chatapp.domain.UserStatus;
import com.example.chatapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 설정
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setStatus(UserStatus.ONLINE);
    }

    @Test
    @DisplayName("사용자 생성 성공")
    void createUser_Success() {
        // Given
        User newUser = new User();
        newUser.setUsername("newuser");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        User result = userService.createUser(newUser);

        // Then
        assertNotNull(result);
        verify(userRepository).findByUsername("newuser");
        verify(userRepository).save(newUser);
    }

    @Test
    @DisplayName("사용자 생성 실패 - 중복된 사용자명")
    void createUser_DuplicateUsername() {
        // Given
        User duplicateUser = new User();
        duplicateUser.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.createUser(duplicateUser));
        verify(userRepository).findByUsername("testuser");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("모든 사용자 조회")
    void findAllUsers() {
        // Given
        List<User> userList = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(userList);

        // When
        List<User> result = userService.findAllUsers();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("ID로 사용자 조회 성공")
    void findUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findUserById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("ID로 사용자 조회 실패 - 존재하지 않는 사용자")
    void findUserById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findUserById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("사용자명으로 사용자 조회 성공")
    void findByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsername("testuser");

        // Then
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("사용자명으로 사용자 조회 실패 - 존재하지 않는 사용자명")
    void findByUsername_NotFound() {
        // Given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.findByUsername("unknown");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByUsername("unknown");
    }

    @Test
    @DisplayName("사용자 상태 업데이트 성공")
    void updateUserStatus_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUserStatus(1L, UserStatus.ONLINE);

        // Then
        assertNotNull(result);

        // 상태가 변경되었는지 확인
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserStatus.ONLINE, userCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("사용자 상태 업데이트 실패 - 존재하지 않는 사용자")
    void updateUserStatus_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.updateUserStatus(999L, UserStatus.OFFLINE));
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 삭제 성공")
    void deleteUser_Success() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("사용자 삭제 실패 - 존재하지 않는 사용자")
    void deleteUser_UserNotFound() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.deleteUser(999L));
        verify(userRepository).existsById(999L);
        verify(userRepository, never()).deleteById(anyLong());
    }
}
