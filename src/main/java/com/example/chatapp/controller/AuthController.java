package com.example.chatapp.controller;

import com.example.chatapp.domain.User;
import com.example.chatapp.exception.UnauthorizedException;
import com.example.chatapp.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
     * @param request 로그인 요청 (username, password)
     * @param response HTTP 응답 객체 (쿠키 설정용)
     * @return 로그인 결과
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String username = request.get("username");
        String password = request.get("password");

        Map<String, Object> authResponse = authService.login(username, password);

        // 세션 토큰을 쿠키에 저장
        String token = (String) authResponse.get("token");
        setCookie(response, token);

        return ResponseEntity.ok(authResponse);
    }

    /**
     * 회원가입 API
     * @param request 회원가입 요청 (username, password)
     * @param response HTTP 응답 객체 (쿠키 설정용)
     * @return 회원가입 결과
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String username = request.get("username");
        String password = request.get("password");

        Map<String, Object> authResponse = authService.signup(username, password);

        // 세션 토큰을 쿠키에 저장
        String token = (String) authResponse.get("token");
        setCookie(response, token);

        return ResponseEntity.ok(authResponse);
    }

    /**
     * 토큰 검증 API
     * @param token 쿠키의 세션 토큰
     * @param authHeader Authorization 헤더
     * @return 토큰 검증 결과
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@CookieValue(name = SESSION_COOKIE_NAME, required = false) String token,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 쿠키에서 토큰을 가져오거나, Authorization 헤더에서 가져옴
        String sessionToken = getTokenFromCookieOrHeader(token, authHeader);

        if (sessionToken == null) {
            throw new UnauthorizedException("인증이 필요합니다");
        }

        Map<String, Object> response = authService.validateToken(sessionToken);
        return ResponseEntity.ok(response);
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
     * @param token 쿠키의 세션 토큰
     * @param authHeader Authorization 헤더
     * @return 현재 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@CookieValue(name = SESSION_COOKIE_NAME, required = false) String token,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 쿠키에서 토큰을 가져오거나, Authorization 헤더에서 가져옴
        String sessionToken = getTokenFromCookieOrHeader(token, authHeader);

        if (sessionToken == null) {
            throw new UnauthorizedException("인증이 필요합니다");
        }

        if (!authService.isValidSession(sessionToken)) {
            throw new UnauthorizedException("세션이 만료되었습니다");
        }

        User user = authService.getUserBySessionToken(sessionToken);
        if (user == null) {
            throw new UnauthorizedException("사용자를 찾을 수 없습니다");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * 쿠키나 헤더에서 세션 토큰 추출
     * @param cookieToken 쿠키에서 추출한 토큰
     * @param authHeader Authorization 헤더
     * @return 세션 토큰 또는 null
     */
    private String getTokenFromCookieOrHeader(String cookieToken, String authHeader) {
        // 쿠키에서 토큰을 가져옴
        if (cookieToken != null) {
            return cookieToken;
        }

        // 또는 Authorization 헤더에서 가져옴
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
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
