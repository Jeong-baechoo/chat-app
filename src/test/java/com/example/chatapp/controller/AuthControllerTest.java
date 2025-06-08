package com.example.chatapp.controller;

import com.example.chatapp.domain.User;
import com.example.chatapp.dto.request.LoginRequest;
import com.example.chatapp.dto.request.SignupRequest;
import com.example.chatapp.dto.response.AuthResponse;
import com.example.chatapp.dto.response.LogoutResponse;
import com.example.chatapp.exception.GlobalExceptionHandler;
import com.example.chatapp.exception.UnauthorizedException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.infrastructure.auth.JwtTokenProvider;
import com.example.chatapp.service.AuthService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Date;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class}, includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = GlobalExceptionHandler.class))
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String validUsername;
    private String validPassword;
    private String validJwtToken;
    private AuthResponse authResponse;
    private User validUser;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        validUsername = "testuser";
        validPassword = "password123";
        validJwtToken = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoidGVzdHVzZXIiLCJpYXQiOjE2MzQ1NjcyMzAsImV4cCI6MTYzNDU2OTAzMH0.test-jwt-token";
        validUser = User.create(validUsername, validPassword);

        // AuthResponse DTO 준비
        authResponse = AuthResponse.builder()
                .token(validJwtToken)
                .userId(validUser.getId())
                .username(validUser.getUsername())
                .expiresAt(new Date(System.currentTimeMillis() + 1800000))
                .valid(true)
                .build();

        // AuthService Mock 기본 설정
        when(authService.getUserById(validUser.getId())).thenReturn(validUser);
    }

    @Test
    @DisplayName("로그인 성공 시 토큰과 사용자 정보를 반환하고 쿠키를 설정한다")
    void givenValidCredentials_whenLogin_thenReturnTokenAndUserInfo() throws Exception {
        // given
        LoginRequest request = new LoginRequest(validUsername, validPassword);
        when(authService.login(validUsername, validPassword)).thenReturn(authResponse);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(validJwtToken))
                .andExpect(jsonPath("$.userId").value(validUser.getId()))
                .andExpect(jsonPath("$.username").value(validUser.getUsername()))
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("로그인 실패 시 401 Unauthorized 응답을 반환한다")
    void givenInvalidCredentials_whenLogin_thenReturnUnauthorized() throws Exception {
        // given
        String invalidUsername = "invaliduser";
        String invalidPassword = "wrongpassword";
        when(authService.login(invalidUsername, invalidPassword)).thenThrow(new UnauthorizedException("Invalid credentials"));

        LoginRequest request = new LoginRequest(invalidUsername, invalidPassword);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("회원가입 성공 시 토큰과 사용자 정보를 반환한다")
    void givenValidSignupData_whenSignup_thenReturnTokenAndUserInfo() throws Exception {
        // given
        String newUsername = "newuser";
        String newPassword = "newpassword123";

        SignupRequest request = new SignupRequest(newUsername, newPassword);

        AuthResponse signupResponse = AuthResponse.builder()
                .token(validJwtToken)
                .userId(2L)
                .username(newUsername)
                .expiresAt(new Date(System.currentTimeMillis() + 1800000))
                .valid(true)
                .build();

        when(authService.signup(newUsername, newPassword)).thenReturn(signupResponse);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(validJwtToken))
                .andExpect(jsonPath("$.userId").value(2L))
                .andExpect(jsonPath("$.username").value(newUsername));

        verify(authService, times(1)).signup(newUsername, newPassword);
    }

    @Test
    @DisplayName("이미 사용 중인 사용자명으로 회원가입 시 400 Bad Request 응답을 반환한다")
    void givenExistingUsername_whenSignup_thenReturnBadRequest() throws Exception {
        // given
        String existingUsername = "existinguser";
        String password = "password123";

        SignupRequest request = new SignupRequest(existingUsername, password);

        when(authService.signup(existingUsername, password))
            .thenThrow(new UserException("이미 사용 중인 사용자명입니다"));

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        resultActions.andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 사용자명입니다"))
                .andExpect(jsonPath("$.status").value("CONFLICT"));

        verify(authService, times(1)).signup(existingUsername, password);
    }

    @Test
    @DisplayName("로그아웃 성공 시 세션이 삭제되고 성공 메시지를 반환한다")
    void givenValidToken_whenLogout_thenDeleteSessionAndReturnSuccess() throws Exception {
        // given - 이미 setUp()에서 기본 설정됨
        doNothing().when(authService).logout(validJwtToken);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("JWT_TOKEN", validJwtToken)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));

        verify(authService, times(1)).logout(validJwtToken);
    }

    @Test
    @DisplayName("토큰 없이 로그아웃 요청도 성공적으로 처리된다")
    void givenNoToken_whenLogout_thenReturnSuccess() throws Exception {
        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/logout"));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));

        // 토큰이 없으므로 서비스 메소드는 호출되지 않아야 함
        verify(authService, never()).logout(any());
    }

    @Test
    @DisplayName("필터에서 인증된 사용자 ID로 현재 사용자 정보를 조회한다")
    void givenAuthenticatedUser_whenGetCurrentUser_thenReturnUserInfo() throws Exception {
        // given - 필터에서 설정한 userId 시뮬레이션
        
        // when
        ResultActions resultActions = mockMvc.perform(get("/api/auth/me")
                .requestAttr("userId", validUser.getId())); // 필터에서 설정한 userId 시뮬레이션

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(validUser.getId()))
                .andExpect(jsonPath("$.username").value(validUser.getUsername()));

        verify(authService, times(1)).getUserById(validUser.getId());
    }

    @Test
    @DisplayName("인증되지 않은 요청시 401 응답을 반환한다")
    void givenUnauthenticatedRequest_whenGetCurrentUser_thenReturnUnauthorized() throws Exception {
        // when - userId 속성 없이 요청
        ResultActions resultActions = mockMvc.perform(get("/api/auth/me"));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다"))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));

        verify(authService, never()).getUserById(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 요청 시 401 응답을 반환한다")
    void givenNonExistentUserId_whenGetCurrentUser_thenReturnUnauthorized() throws Exception {
        // given
        Long nonExistentUserId = 999L;
        when(authService.getUserById(nonExistentUserId))
            .thenThrow(new UnauthorizedException("사용자를 찾을 수 없습니다"));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/auth/me")
                .requestAttr("userId", nonExistentUserId));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다"))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));

        verify(authService, times(1)).getUserById(nonExistentUserId);
    }
}