# WebSocket + STOMP 설정 가이드

## 백엔드 (Spring Boot) 설정 예시

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독할 수 있는 prefix
        registry.enableSimpleBroker("/topic", "/queue");
        
        // 클라이언트가 서버로 메시지를 보낼 때 사용하는 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }
}

@Controller
public class ChatController {
    
    @MessageMapping("/message.send")
    @SendTo("/topic/room/{roomId}")
    public MessageDTO sendMessage(@Payload MessageDTO message) {
        return message;
    }
    
    @MessageMapping("/room.enter")
    @SendTo("/topic/room/{roomId}")
    public NotificationDTO enterRoom(@Payload RoomEnterDTO enterData) {
        return new NotificationDTO(enterData.getUsername() + "님이 입장했습니다.");
    }
    
    @MessageMapping("/room.leave")
    @SendTo("/topic/room/{roomId}")
    public NotificationDTO leaveRoom(@Payload RoomLeaveDTO leaveData) {
        return new NotificationDTO(leaveData.getUsername() + "님이 퇴장했습니다.");
    }
}
```

## 프론트엔드 현재 설정 체크리스트

### ✅ 올바른 설정
1. SockJS 사용 (백엔드에서 `.withSockJS()` 사용 시)
2. 구독 경로: `/topic/room/${roomId}` (올바름)
3. 에러 구독: `/queue/errors` (올바름)

### ❌ 수정이 필요했던 부분
1. 메시지 전송 destination에 `/app` prefix 추가
   - `/message.send` → `/app/message.send`
   - `/room.enter` → `/app/room.enter`
   - `/room.leave` → `/app/room.leave`

## 전체 통신 흐름

1. **연결**: 
   - 클라이언트: `new SockJS('http://localhost:8080/ws')`
   - 서버: `/ws` 엔드포인트로 핸드셰이크

2. **구독**:
   - 클라이언트: `subscribe('/topic/room/