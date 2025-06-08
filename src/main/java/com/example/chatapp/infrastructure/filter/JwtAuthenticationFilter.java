package com.example.chatapp.infrastructure.filter;

import com.example.chatapp.infrastructure.auth.JwtTokenProvider;
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
 * JWT 기반 인증을 처리하는 필터
 * 모든 요청에 대해 JWT 토큰 인증을 검사하고 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    private static final String JWT_COOKIE_NAME = "JWT_TOKEN";

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
            // 쿠키나 헤더에서 JWT 토큰 추출
            String jwtToken = extractJwtToken(request);
            if (jwtToken == null) {
                handleAuthenticationFailure(response, "인증이 필요합니다");
                return;
            }

            // JWT 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(jwtToken)) {
                handleAuthenticationFailure(response, "유효하지 않은 토큰입니다");
                return;
            }

            // JWT에서 사용자 ID 추출
            Long userId = jwtTokenProvider.getUserId(jwtToken);
            log.debug("JWT 인증 성공: userId=[PROTECTED]");
            
            // 요청 속성에 사용자 정보 추가
            request.setAttribute("userId", userId);
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            handleAuthenticationFailure(response, e.getMessage());
        }
    }

    /**
     * 인증 실패 처리를 일관된 형식으로 반환
     */
    private void handleAuthenticationFailure(HttpServletResponse response, String message) throws IOException {
        log.warn("JWT 인증 실패");
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
               path.startsWith("/api/auth/logout") ||
               path.startsWith("/api/health") ||
               path.startsWith("/socket");

        if (isPublic) {
            log.debug("공개 경로 접근: {}", path);
        }
        return isPublic;
    }

    /**
     * 요청에서 JWT 토큰 추출
     * 쿠키 또는 헤더에서 토큰을 찾음
     */
    private String extractJwtToken(HttpServletRequest request) {
        // Authorization 헤더에서 추출 (우선순위)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.debug("Authorization 헤더에서 JWT 토큰 추출 성공");
            return token;
        }

        // 쿠키에서 추출
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                    log.debug("쿠키에서 JWT 토큰 추출 성공");
                    return cookie.getValue();
                }
            }
        }

        log.debug("JWT 토큰 추출 실패: 쿠키 또는 Authorization 헤더에서 토큰을 찾을 수 없음");
        return null;
    }
}
