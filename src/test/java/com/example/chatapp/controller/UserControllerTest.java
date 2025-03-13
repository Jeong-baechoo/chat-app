package com.example.chatapp.controller;

import com.example.chatapp.domain.User;
import com.example.chatapp.service.impl.UserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private UserServiceImpl userServiceImpl;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("사용자 생성 테스트")
    public void testCreateUser() throws Exception {
        // Given
        User user = User.builder()
                .username("testuser")
                .password("password123")
                .build();



        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("사용자 조회 테스트")
    public void testGetUserById() throws Exception {
        // Given
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .build();


        // When & Then
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("testuser"));
    }


}
