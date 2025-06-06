package com.example.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커가 구독(subscribe) 관련 메시지를 처리
        config.enableSimpleBroker("/topic");

        // 클라이언트가 메시지를 보낼 때 사용하는 prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 엔드포인트 설정
        registry.addEndpoint("/ws") // 클라이언트가 WebSocket 연결을 요청할 URL
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // 순수 WebSocket 엔드포인트 추가 (k6 테스트용)
        registry.addEndpoint("/ws-raw")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // 메시지 버퍼 크기 최적화
        registration.setMessageSizeLimit(128 * 1024); // 기본값 64K에서 증가
        registration.setSendBufferSizeLimit(512 * 1024); // 버퍼 크기 조정

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 클라이언트로부터 메시지를 받는 스레드 풀 최적화
        registration.taskExecutor()
                .corePoolSize(16)       // 증가
                .maxPoolSize(32)        // 증가
                .queueCapacity(200)     // 증가
                .keepAliveSeconds(60);
    }
    
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // 클라이언트로 메시지를 보내는 스레드 풀 최적화
        registration.taskExecutor()
                .corePoolSize(16)       // 증가
                .maxPoolSize(32)        // 증가
                .queueCapacity(200)     // 증가
                .keepAliveSeconds(60);
    }
}
