package com.example.chatapp.controller;

import com.example.chatapp.domain.User;
import com.example.chatapp.dto.request.LoginRequest;
import com.example.chatapp.dto.request.SignupRequest;
import com.example.chatapp.dto.response.AuthResponse;
import com.example.chatapp.exception.GlobalExceptionHandler;
import com.example.chatapp.exception.UnauthorizedException;
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

        // JWT Mock 기본 설정
        when(jwtTokenProvider.validateToken(validJwtToken)).thenReturn(true);
        when(jwtTokenProvider.getUserId(validJwtToken)).thenReturn(validUser.getId());
        when(jwtTokenProvider.getUsername(validJwtToken)).thenReturn(validUser.getUsername());
        when(jwtTokenProvider.extractToken(validJwtToken)).thenReturn(validJwtToken);
        when(jwtTokenProvider.extractToken("Bearer " + validJwtToken)).thenReturn(validJwtToken);
        when(authService.isValidToken(validJwtToken)).thenReturn(true);
        when(authService.getUserByToken(validJwtToken)).thenReturn(validUser);
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
            .thenThrow(new IllegalArgumentException("이미 사용 중인 사용자명입니다"));

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
    @DisplayName("토큰 없이 검증 요청 시 401 Unauthorized 응답을 반환한다")
    void givenNoToken_whenValidateToken_thenReturnUnauthorized() throws Exception {
        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/validate"));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다"))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));

        verify(authService, never()).validateToken(any());
    }

    @Test
    @DisplayName("유효한 토큰 검증 시 사용자 정보를 반환한다")
    void givenValidToken_whenValidateToken_thenReturnUserInfo() throws Exception {
        // given
        AuthResponse validationResponse = AuthResponse.builder()
                .token(validJwtToken)
                .userId(validUser.getId())
                .username(validUser.getUsername())
                .valid(true)
                .expiresAt(new Date(System.currentTimeMillis() + 1800000))
                .build();

        when(authService.validateToken(validJwtToken)).thenReturn(validationResponse);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/validate")
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", validJwtToken)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(validUser.getId()))
                .andExpect(jsonPath("$.username").value(validUser.getUsername()))
                .andExpect(jsonPath("$.valid").value(true));

        verify(authService, times(1)).validateToken(validJwtToken);
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 401 Unauthorized 응답을 반환한다")
    void givenInvalidJwtToken_whenValidateToken_thenReturnUnauthorized() throws Exception {
        // given
        String invalidJwtToken = "invalid-token";
        when(authService.validateToken(invalidJwtToken))
            .thenThrow(new UnauthorizedException("유효하지 않은 세션입니다"));

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/validate")
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", invalidJwtToken)));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("유효하지 않은 세션입니다"))
                .andExpect(jsonPath("$.status").value("UNAUTHORIZED"));

        verify(authService, times(1)).validateToken(invalidJwtToken);
    }

    @Test
    @DisplayName("Authorization 헤더에서 토큰 검증이 정상적으로 동작한다")
    void givenValidTokenInHeader_whenValidateToken_thenReturnUserInfo() throws Exception {
        // given
        AuthResponse validationResponse = AuthResponse.builder()
                .token(validJwtToken)
                .userId(validUser.getId())
                .username(validUser.getUsername())
                .valid(true)
                .expiresAt(new Date(System.currentTimeMillis() + 1800000))
                .build();

        when(authService.validateToken(validJwtToken)).thenReturn(validationResponse);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/validate")
                .header("Authorization", "Bearer " + validJwtToken));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(validUser.getId()))
                .andExpect(jsonPath("$.username").value(validUser.getUsername()))
                .andExpect(jsonPath("$.valid").value(true));

        verify(authService, times(1)).validateToken(validJwtToken);
    }

    @Test
    @DisplayName("로그아웃 성공 시 세션이 삭제되고 성공 메시지를 반환한다")
    void givenValidToken_whenLogout_thenDeleteSessionAndReturnSuccess() throws Exception {
        // given - 이미 setUp()에서 기본 설정됨
        doNothing().when(authService).logout(validJwtToken);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", validJwtToken)));

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
    @DisplayName("유효한 토큰으로 현재 사용자 정보를 조회한다")
    void givenValidToken_whenGetCurrentUser_thenReturnUserInfo() throws Exception {
        // given - 이미 setUp()에서 기본 설정됨

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/auth/me")
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", validJwtToken)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(validUser.getId()))
                .andExpect(jsonPath("$.username").value(validUser.getUsername()));

        verify(authService, times(1)).isValidToken(validJwtToken);
        verify(authService, times(1)).getUserByToken(validJwtToken);
    }

    @Test
    @DisplayName("토큰이 유효하지 않을 때 현재 사용자 조회 실패")
    void givenInvalidJwtToken_whenGetCurrentUser_thenReturnUnauthorized() throws Exception {
        // given
        String invalidJwtToken = "invalid-token";
        when(authService.isValidToken(invalidJwtToken)).thenReturn(false);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/auth/me")
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", invalidJwtToken)));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));

        verify(authService, never()).getUserByToken(any());
    }

    @Test
    @DisplayName("토큰 없이 현재 사용자 정보 조회 시 401 응답을 반환한다")
    void givenNoToken_whenGetCurrentUser_thenReturnUnauthorized() throws Exception {
        // when
        ResultActions resultActions = mockMvc.perform(get("/api/auth/me"));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("인증이 필요합니다"))
                .andExpect(jsonPath("$.status").value(401));

        // 토큰이 없으므로 서비스 메소드는 호출되지 않아야 함
        verify(authService, never()).isValidToken(any());
        verify(authService, never()).getUserByToken(any());
    }

    @Test
    @DisplayName("토큰은 유효하지만 사용자를 찾을 수 없는 경우")
    void givenValidTokenButNoUser_whenGetCurrentUser_thenReturnUnauthorized() throws Exception {
        // given
        String validJwtTokenNoUser = "valid-jwt-token-no-user";

        // JwtTokenProvider 설정 - 토큰을 무효한 것으로 처리
        when(jwtTokenProvider.validateToken(validJwtTokenNoUser)).thenReturn(false);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/auth/me")
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", validJwtTokenNoUser)));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("유효하지 않은 토큰입니다"))
                .andExpect(jsonPath("$.status").value(401));

        verify(jwtTokenProvider, times(1)).validateToken(validJwtTokenNoUser);
        verify(authService, never()).isValidToken(any());
        verify(authService, never()).getUserByToken(any());
    }
}
