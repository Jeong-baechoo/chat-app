package com.example.chatapp.controller;

import com.example.chatapp.config.WebFilterConfig;
import com.example.chatapp.domain.LoginSession;
import com.example.chatapp.domain.User;
import com.example.chatapp.exception.UnauthorizedException;
import com.example.chatapp.infrastructure.session.SessionStore;
import com.example.chatapp.service.AuthService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SessionStore sessionStore;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String validUsername;
    private String validPassword;
    private String validToken;
    private Map<String, Object> authResponseMap;
    private User validUser;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        validUsername = "testuser";
        validPassword = "password123";
        validToken = UUID.randomUUID().toString();
        validUser = User.builder()
                .id(1L)
                .username(validUsername)
                .password("encodedPassword")
                .build();

        // 인증 응답 맵 준비
        authResponseMap = new HashMap<>();
        authResponseMap.put("token", validToken);
        authResponseMap.put("userId", validUser.getId());
        authResponseMap.put("username", validUser.getUsername());
        authResponseMap.put("expiresAt", System.currentTimeMillis() + 1800000);

//        // Mock 기본 설정
//        LoginSession validSession = new LoginSession(validUser.getId(), System.currentTimeMillis() + 1800000);
//        when(sessionStore.getSession(validToken)).thenReturn(validSession);
//        when(authService.isValidSession(validToken)).thenReturn(true);
//        when(authService.getUserBySessionToken(validToken)).thenReturn(validUser);
    }

    @Test
    @DisplayName("로그인 성공 시 토큰과 사용자 정보를 반환하고 쿠키를 설정한다")
    void givenValidCredentials_whenLogin_thenReturnTokenAndUserInfo() throws Exception {
        // given
        when(authService.login(validUsername, validPassword)).thenReturn(authResponseMap);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", validUsername, "password", validPassword))));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(validToken))
                .andExpect(jsonPath("$.userId").value(validUser.getId()))
                .andExpect(jsonPath("$.username").value(validUser.getUsername()));
    }

    @Test
    @DisplayName("로그인 실패 시 401 Unauthorized 응답을 반환한다")
    void givenInvalidCredentials_whenLogin_thenReturnUnauthorized() throws Exception {
        // given
        String invalidUsername = "invaliduser";
        String invalidPassword = "wrongpassword";
        when(authService.login(invalidUsername, invalidPassword)).thenThrow(new UnauthorizedException("Invalid credentials"));

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", invalidUsername, "password", invalidPassword))));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Invalid credentials"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("회원가입 성공 시 토큰과 사용자 정보를 반환한다")
    void givenValidSignupData_whenSignup_thenReturnTokenAndUserInfo() throws Exception {
        // given
        String newUsername = "newuser";
        String newPassword = "newpassword123";

        Map<String, Object> signupResponse = new HashMap<>();
        signupResponse.put("token", validToken);
        signupResponse.put("userId", 2L);
        signupResponse.put("username", newUsername);
        signupResponse.put("expiresAt", System.currentTimeMillis() + 1800000);

        when(authService.signup(newUsername, newPassword)).thenReturn(signupResponse);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", newUsername, "password", newPassword))));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(validToken))
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

        when(authService.signup(existingUsername, password))
            .thenThrow(new IllegalArgumentException("이미 사용 중인 사용자명입니다"));

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", existingUsername, "password", password))));

        // then
        resultActions.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("이미 사용 중인 사용자명입니다"))
                .andExpect(jsonPath("$.status").value(400));

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
                .andExpect(jsonPath("$.error").value("인증이 필요합니다"))
                .andExpect(jsonPath("$.status").value(401));

        verify(authService, never()).validateToken(any());
    }

    @Test
    @DisplayName("유효한 토큰 검증 시 사용자 정보를 반환한다")
    void givenValidToken_whenValidateToken_thenReturnUserInfo() throws Exception {
        // given
        Map<String, Object> validationResponse = new HashMap<>();
        validationResponse.put("userId", validUser.getId());
        validationResponse.put("username", validUser.getUsername());
        validationResponse.put("valid", true);

        when(authService.validateToken(validToken)).thenReturn(validationResponse);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/validate")
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", validToken)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(validUser.getId()))
                .andExpect(jsonPath("$.username").value(validUser.getUsername()))
                .andExpect(jsonPath("$.valid").value(true));

        verify(authService, times(1)).validateToken(validToken);
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 401 Unauthorized 응답을 반환한다")
    void givenInvalidToken_whenValidateToken_thenReturnUnauthorized() throws Exception {
        // given
        String invalidToken = "invalid-token";
        when(authService.validateToken(invalidToken))
            .thenThrow(new UnauthorizedException("유효하지 않은 세션입니다"));

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/validate")
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", invalidToken)));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("유효하지 않은 세션입니다"))
                .andExpect(jsonPath("$.status").value(401));

        verify(authService, times(1)).validateToken(invalidToken);
    }

    @Test
    @DisplayName("Authorization 헤더에서 토큰 검증이 정상적으로 동작한다")
    void givenValidTokenInHeader_whenValidateToken_thenReturnUserInfo() throws Exception {
        // given
        Map<String, Object> validationResponse = new HashMap<>();
        validationResponse.put("userId", validUser.getId());
        validationResponse.put("username", validUser.getUsername());
        validationResponse.put("valid", true);

        when(authService.validateToken(validToken)).thenReturn(validationResponse);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/validate")
                .header("Authorization", "Bearer " + validToken));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(validUser.getId()))
                .andExpect(jsonPath("$.username").value(validUser.getUsername()))
                .andExpect(jsonPath("$.valid").value(true));

        verify(authService, times(1)).validateToken(validToken);
    }

    @Test
    @DisplayName("로그아웃 성공 시 세션이 삭제되고 성공 메시지를 반환한다")
    void givenValidToken_whenLogout_thenDeleteSessionAndReturnSuccess() throws Exception {
        // given - 이미 setUp()에서 기본 설정됨
        doNothing().when(authService).logout(validToken);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", validToken)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));

        verify(authService, times(1)).logout(validToken);
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
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", validToken)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(validUser.getId()))
                .andExpect(jsonPath("$.username").value(validUser.getUsername()));

        verify(authService, times(1)).isValidSession(validToken);
        verify(authService, times(1)).getUserBySessionToken(validToken);
    }

    @Test
    @DisplayName("토큰이 유효하지 않을 때 현재 사용자 조회 실패")
    void givenInvalidToken_whenGetCurrentUser_thenReturnUnauthorized() throws Exception {
        // given
        String invalidToken = "invalid-token";
        when(authService.isValidSession(invalidToken)).thenReturn(false);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/auth/me")
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", invalidToken)));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("세션이 만료되었습니다"))
                .andExpect(jsonPath("$.status").value(401));

        verify(authService, never()).getUserBySessionToken(any());
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
        verify(authService, never()).isValidSession(any());
        verify(authService, never()).getUserBySessionToken(any());
    }

    @Test
    @DisplayName("세션은 유효하지만 사용자를 찾을 수 없는 경우")
    void givenValidTokenButNoUser_whenGetCurrentUser_thenReturnUnauthorized() throws Exception {
        // given
        String validTokenNoUser = UUID.randomUUID().toString();
        LoginSession validSession = new LoginSession(999L, System.currentTimeMillis() + 1800000);

        when(sessionStore.getSession(validTokenNoUser)).thenReturn(validSession);
        when(authService.isValidSession(validTokenNoUser)).thenReturn(true);
        when(authService.getUserBySessionToken(validTokenNoUser)).thenReturn(null);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/auth/me")
                .cookie(new jakarta.servlet.http.Cookie("SESSION_TOKEN", validTokenNoUser)));

        // then
        resultActions.andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("사용자를 찾을 수 없습니다"))
                .andExpect(jsonPath("$.status").value(401));

        verify(authService, times(1)).isValidSession(validTokenNoUser);
        verify(authService, times(1)).getUserBySessionToken(validTokenNoUser);
    }
}
