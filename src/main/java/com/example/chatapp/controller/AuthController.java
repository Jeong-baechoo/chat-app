package com.example.chatapp.controller;

import com.example.chatapp.domain.User;
import com.example.chatapp.dto.request.LoginRequest;
import com.example.chatapp.dto.request.SignupRequest;
import com.example.chatapp.dto.response.AuthResponse;
import com.example.chatapp.dto.response.LogoutResponse;
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


/**
 * JWT 기반 인증 관련 API 컨트롤러
 * 로그인, 회원가입, 로그아웃, 사용자 정보 조회 등의 엔드포인트 제공
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final String JWT_COOKIE_NAME = "JWT_TOKEN";
    private static final int COOKIE_MAX_AGE = 30 * 60; // 30분

    /**
     * 로그인 API - JWT 토큰 발급
     * @param request 로그인 요청 DTO (검증용)
     * @param response HTTP 응답 객체 (JWT 쿠키 설정용)
     * @return JWT 토큰과 사용자 정보
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request.getUsername(), request.getPassword());
        setJwtCookie(response, authResponse.getToken());
        return ResponseEntity.ok(authResponse);
    }

    /**
     * 회원가입 API - 회원가입 후 JWT 토큰 자동 발급
     * @param request 회원가입 요청 DTO (검증용)
     * @param response HTTP 응답 객체 (JWT 쿠키 설정용)
     * @return JWT 토큰과 생성된 사용자 정보
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody @Valid SignupRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.signup(request.getUsername(), request.getPassword());
        setJwtCookie(response, authResponse.getToken());
        return ResponseEntity.ok(authResponse);
    }


    /**
     * 로그아웃 API - JWT 쿠키 삭제
     * JWT는 stateless이므로 서버에서 별도 처리 없이 쿠키만 삭제
     * @param response HTTP 응답 객체 (JWT 쿠키 삭제용)
     * @return 로그아웃 결과
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // JWT 쿠키 삭제
        clearJwtCookie(response);

//        authService.logout();

        return ResponseEntity.ok(Map.of(
                "message", "로그아웃되었습니다",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));

    /**
     * 현재 사용자 정보 조회 API
     * JwtAuthenticationFilter에서 이미 인증된 사용자 ID를 사용
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
     * JWT 쿠키 설정
     *
     * @param response HTTP 응답
     * @param jwtToken JWT 토큰 값
     */
    private void setJwtCookie(HttpServletResponse response, String jwtToken) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, jwtToken);
        cookie.setHttpOnly(true);  // XSS 공격 방지
        cookie.setSecure(false); // HTTPS에서만 전송 (개발 환경에서는 false)
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    /**
     * JWT 쿠키 삭제
     * @param response HTTP 응답
     */
    private void clearJwtCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, null);
        cookie.setMaxAge(0);  // 즉시 만료
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
