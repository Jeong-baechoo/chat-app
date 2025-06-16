package com.example.chatapp.controller;

import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.GlobalExceptionHandler;
import com.example.chatapp.infrastructure.auth.JwtTokenProvider;
import com.example.chatapp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {UserController.class},
    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GlobalExceptionHandler.class))
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("전체 사용자 조회 성공 테스트")
    public void testGetAllUsers() throws Exception {
        // Given
        List<UserResponse> userResponses = List.of(
                UserResponse.builder().id(1L).username("testuser1").build(),
                UserResponse.builder().id(2L).username("testuser2").build()
        );
        when(userService.findAllUsers()).thenReturn(userResponses);

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].username").value("testuser1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].username").value("testuser2"));
    }

    @Test
    @DisplayName("사용자 ID로 조회 성공 테스트")
    public void givenValidUserId_whenGetUserById_thenReturnUserInfo() throws Exception {
        // Given
        Long userId = 1L;
        UserResponse userResponse = UserResponse.builder()
                .id(userId)
                .username("testuser")
                .build();
        when(userService.findUserById(userId)).thenReturn(userResponse);

        // When & Then
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("사용자명으로 조회 성공 테스트")
    public void givenValidUsername_whenGetUserByUsername_thenReturnUserInfo() throws Exception {
        // Given
        String username = "testuser";
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username(username)
                .build();
        when(userService.findByUsername(username)).thenReturn(Optional.of(userResponse));

        // When & Then
        mockMvc.perform(get("/api/users/username/{username}", username))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    @DisplayName("존재하지 않는 사용자명으로 조회 테스트")
    public void givenInvalidRoomId_whenGetRoomById_thenReturnNotFound() throws Exception {
        // Given
        String username = "nonexistent";
        when(userService.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/users/username/{username}", username))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("로그인된 사용자 목록 조회 테스트")
    public void givenValidRoomRequest_whenCreateRoom_thenReturnCreatedRoomWithLocation() throws Exception {
        // Given
        List<UserResponse> loggedInUsers = List.of(
                UserResponse.builder().id(1L).username("online1").build(),
                UserResponse.builder().id(3L).username("online2").build()
        );
        when(userService.findLoggedInUsers()).thenReturn(loggedInUsers);

        // When & Then
        mockMvc.perform(get("/api/users/isLoggedIn"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].username").value("online1"))
                .andExpect(jsonPath("$[1].id").value(3L))
                .andExpect(jsonPath("$[1].username").value("online2"));
    }
}
