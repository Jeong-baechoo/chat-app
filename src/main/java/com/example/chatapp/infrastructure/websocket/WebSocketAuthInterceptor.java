package com.example.chatapp.infrastructure.websocket;

import com.example.chatapp.infrastructure.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * WebSocket Handshake 시 JWT 토큰 기반 인증을 처리하는 인터셉터
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        log.debug("WebSocket handshake 요청: {}", request.getURI());
        
        // 보안상 쿼리 파라미터 사용 금지 - 헤더에서만 토큰 추출
        String token = extractTokenFromHeaders(request);
        
        if (token == null) {
            log.warn("WebSocket handshake 실패: 토큰이 없음");
            return false;
        }
        
        // JWT 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket handshake 실패: 유효하지 않은 JWT 토큰");
            return false;
        }
        
        try {
            // JWT에서 사용자 정보 추출
            Long userId = jwtTokenProvider.getUserId(token);
            String username = jwtTokenProvider.getUsername(token);
            
            // WebSocket 세션에 사용자 정보 저장
            attributes.put("userId", userId);
            attributes.put("username", username);
            attributes.put("jwtToken", token);
            
            log.info("WebSocket handshake 성공: userId={}, username={}", userId, username);
        } catch (Exception e) {
            log.warn("WebSocket handshake 실패: JWT 토큰 파싱 오류 - {}", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake 후 오류 발생", exception);
        }
    }

    /**
     * HTTP 헤더에서 JWT 토큰 추출 (보안 강화)
     * Authorization 헤더 또는 HttpOnly 쿠키에서만 토큰을 추출
     * 쿼리 파라미터는 보안상 위험하므로 사용하지 않음
     */
    private String extractTokenFromHeaders(ServerHttpRequest request) {
        // Authorization 헤더에서 추출
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                log.debug("Authorization 헤더에서 토큰 추출 성공");
                return authHeader.substring(7);
            }
        }
        
        // Cookie 헤더에서 추출
        List<String> cookieHeaders = request.getHeaders().get("Cookie");
        if (cookieHeaders != null && !cookieHeaders.isEmpty()) {
            String cookieHeader = cookieHeaders.get(0);
            String[] cookies = cookieHeader.split(";");
            
            for (String cookie : cookies) {
                String[] cookieParts = cookie.trim().split("=", 2);
                if (cookieParts.length == 2 && "SESSION_TOKEN".equals(cookieParts[0])) {
                    log.debug("Cookie에서 토큰 추출 성공");
                    return cookieParts[1];
                }
            }
        }
        
        return null;
    }
}