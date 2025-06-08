package com.example.chatapp.controller;

import com.example.chatapp.domain.User;
import com.example.chatapp.dto.request.LoginRequest;
import com.example.chatapp.dto.request.SignupRequest;
import com.example.chatapp.dto.response.AuthResponse;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.UnauthorizedException;
import com.example.chatapp.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증 관련 API 컨트롤러
 * 로그인, 회원가입, 토큰 검증, 로그아웃 등의 엔드포인트 제공
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final String SESSION_COOKIE_NAME = "SESSION_TOKEN";
    private static final int COOKIE_MAX_AGE = 30 * 60;

    /**
     * 로그인 API
     * @param request 로그인 요청 DTO (검증용)
     * @param response HTTP 응답 객체 (쿠키 설정용)
     * @return 로그인 결과
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request.getUsername(), request.getPassword());
        setCookie(response, authResponse.getToken());
        return ResponseEntity.ok(authResponse);
    }

    /**
     * 회원가입 API
     * @param request 회원가입 요청 DTO (검증용)
     * @param response HTTP 응답 객체 (쿠키 설정용)
     * @return 회원가입 결과
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid SignupRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.signup(request.getUsername(), request.getPassword());
        setCookie(response, authResponse.getToken());
        return ResponseEntity.ok(authResponse);
    }


    /**
     * 로그아웃 API
     * @param token 쿠키의 세션 토큰
     * @param response HTTP 응답 객체 (쿠키 삭제용)
     * @return 로그아웃 결과
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = SESSION_COOKIE_NAME, required = false) String token,
                                    HttpServletResponse response) {
        if (token != null) {
            // 세션 삭제
            authService.logout(token);

            // 쿠키 삭제
            deleteCookie(response, SESSION_COOKIE_NAME);
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "로그아웃 성공"));
    }

    /**
     * 현재 사용자 정보 조회 API
     * 필터에서 이미 인증된 사용자 ID를 사용
     * @param request HTTP 요청 객체 (필터에서 설정한 userId 속성 사용)
     * @return 현재 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(HttpServletRequest request) {
        // 필터에서 설정한 사용자 ID 추출
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("인증이 필요합니다");
        }

        User user = authService.getUserById(userId);

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();

        return ResponseEntity.ok(response);
    }



    /**
     * 쿠키 설정
     *
     * @param response HTTP 응답
     * @param value    쿠키 값
     */
    private void setCookie(HttpServletResponse response, String value) {
        Cookie cookie = new Cookie(AuthController.SESSION_COOKIE_NAME, value);
        cookie.setHttpOnly(true);  // 자바스크립트에서 접근 불가
        cookie.setMaxAge(AuthController.COOKIE_MAX_AGE);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    /**
     * 쿠키 삭제
     * @param response HTTP 응답
     * @param name 삭제할 쿠키 이름
     */
    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);  // 즉시 만료
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
