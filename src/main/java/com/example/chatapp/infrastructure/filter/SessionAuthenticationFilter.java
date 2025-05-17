package com.example.chatapp.infrastructure.filter;

import com.example.chatapp.domain.LoginSession;
import com.example.chatapp.infrastructure.session.SessionStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 세션 기반 인증을 처리하는 필터
 * 모든 요청에 대해 인증을 검사하고 처리
 * AuthInterceptor와의 중복 기능을 통합
 */
//@Component
@RequiredArgsConstructor
@Slf4j
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private final SessionStore sessionStore;
    private final ObjectMapper objectMapper;

    private static final String SESSION_COOKIE_NAME = "SESSION_TOKEN";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // OPTIONS 요청은 CORS preflight 요청이므로 인증 검사 없이 통과시킴
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            // CORS 헤더 설정
            response.setHeader("Access-Control-Allow-Origin", "http://localhost:3000");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "3600");

            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 인증이 필요없는 경로는 패스
        if (isPublicPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 쿠키나 헤더에서 세션 토큰 추출
            String sessionToken = extractSessionToken(request);
            if (sessionToken == null) {
                handleAuthenticationFailure(response, "인증이 필요합니다");
                return;
            }

            // 세션 유효성 검증
            LoginSession session = sessionStore.getSession(sessionToken);
            if (session == null || session.isExpired()) {
                if (session != null) {
                    // 만료된 세션은 제거
                    sessionStore.removeSession(sessionToken);
                }
                handleAuthenticationFailure(response, "세션이 만료되었습니다");
                return;
            }
            log.debug("인증 성공: userId={}", session.getUserId());
            // 요청 속성에 사용자 정보 추가
            request.setAttribute("userId", session.getUserId());
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            handleAuthenticationFailure(response, e.getMessage());
        }
    }

    /**
     * 인증 실패 처리를 일관된 형식으로 반환
     */
    private void handleAuthenticationFailure(HttpServletResponse response, String message) throws IOException {
        log.debug("인증 실패: {}", message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 공개 접근 경로인지 확인
     */
    private boolean isPublicPath(String path) {
        boolean isPublic = path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/signup") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/validate") ||
               path.startsWith("/api/auth/logout") ||
               path.startsWith("/api/health") ||
               path.startsWith("/socket");

        if (isPublic) {
            log.debug("공개 경로 접근: {}", path);
        }
        return isPublic;
    }

    /**
     * 요청에서 세션 토큰 추출
     * 쿠키 또는 헤더에서 토큰을 찾음
     */
    private String extractSessionToken(HttpServletRequest request) {
        // 쿠키에서 추출
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    log.debug("쿠키에서 세션 토큰 추출 성공: {}", cookie.getValue().substring(0, 6) + "...");
                    return cookie.getValue();
                }
            }
        }

        // 또는 Authorization 헤더에서 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("Authorization 헤더에서 세션 토큰 추출 성공: {}", token.substring(0, 6) + "...");
            return token;
        }

        log.debug("세션 토큰 추출 실패: 쿠키 또는 Authorization 헤더에서 토큰을 찾을 수 없음");
        return null;
    }
}
