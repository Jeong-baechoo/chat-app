package com.example.chatapp.controller;

import com.example.chatapp.domain.User;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.infrastructure.filter.SessionAuthenticationFilter;
import com.example.chatapp.infrastructure.session.SessionStore;
import com.example.chatapp.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SessionAuthenticationFilter sessionAuthenticationFilter;

    @MockitoBean
    private SessionStore sessionStore;

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
                .andExpect(status().isOk());
    }


}
