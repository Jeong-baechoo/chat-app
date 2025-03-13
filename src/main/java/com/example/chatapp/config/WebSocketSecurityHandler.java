package com.example.chatapp.config;

import com.example.chatapp.security.SimpleJwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketSecurityHandler implements WebSocketMessageBrokerConfigurer {

    private final SimpleJwtProvider jwtProvider;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 연결 요청 시 인증 토큰 추출
                    List<String> authorization = accessor.getNativeHeader("Authorization");

                    if (authorization != null && !authorization.isEmpty()) {
                        String bearerToken = authorization.get(0);
                        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
                            String jwt = bearerToken.substring(7);
                            if (jwtProvider.validateToken(jwt)) {
                                // 토큰에서 사용자 정보 추출
                                Long userId = jwtProvider.extractUserId(jwt);
                                String username = jwtProvider.extractUsername(jwt);

                                // 세션 속성에 사용자 정보 저장
                                accessor.getSessionAttributes().put("userId", userId);
                                accessor.getSessionAttributes().put("username", username);
                                log.info("WebSocket 연결: 사용자 인증 성공 - userId={}, username={}", userId, username);
                            } else {
                                log.warn("WebSocket 연결: 유효하지 않은 토큰");
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
