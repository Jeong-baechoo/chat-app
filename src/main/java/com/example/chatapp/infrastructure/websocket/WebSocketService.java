package com.example.chatapp.infrastructure.websocket;

import com.example.chatapp.infrastructure.message.ChatEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * WebSocket을 통한 실시간 메시지 전송 서비스
 * Kafka Consumer에서 받은 이벤트를 WebSocket 클라이언트들에게 브로드캐스트
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 채팅방의 모든 참여자에게 이벤트 브로드캐스트
     */
    public void broadcastToRoom(Long roomId, ChatEvent event) {
        try {
            String destination = "/topic/room/" + roomId;

            // WebSocket 메시지 포맷 변환
            Map<String, Object> message = convertToWebSocketMessage(event);

            // 채팅방 구독자들에게 메시지 전송
            messagingTemplate.convertAndSend(destination, message);

            if (log.isDebugEnabled()) {
                log.debug("WebSocket 브로드캐스트 완료: roomId={}, eventType={}",
                        roomId, event.getEventType());
            }

        } catch (Exception e) {
            log.error("WebSocket 브로드캐스트 실패: roomId={}, eventType={}, error={}",
                    roomId, event.getEventType(), e.getMessage(), e);
        }
    }

    /**
     * 특정 채팅방의 모든 참여자에게 배치 이벤트 브로드캐스트
     */
    public void broadcastBatchToRoom(Long roomId, List<ChatEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        try {
            String destination = "/topic/room/" + roomId;

            // WebSocket 메시지 포맷 변환
            List<Map<String, Object>> messages = events.stream()
                    .map(this::convertToWebSocketMessage)
                    .collect(Collectors.toList());

            // 배치 메시지로 한 번에 전송
            Map<String, Object> batchMessage = new HashMap<>();
            batchMessage.put("type", "BATCH");
            batchMessage.put("messages", messages);
            batchMessage.put("count", messages.size());
            batchMessage.put("roomId", roomId);

            // 채팅방 구독자들에게 메시지 전송
            messagingTemplate.convertAndSend(destination, batchMessage);

            if (log.isDebugEnabled()) {
                log.debug("WebSocket 배치 브로드캐스트 완료: roomId={}, 메시지 수={}",
                        roomId, events.size());
            }

        } catch (Exception e) {
            log.error("WebSocket 배치 브로드캐스트 실패: roomId={}, 이벤트 수={}, error={}",
                    roomId, events.size(), e.getMessage(), e);
        }
    }

    /**
     * 특정 사용자에게 개인 메시지 전송
     */
    public void sendToUser(Long userId, ChatEvent event) {
        try {
            String destination = "/queue/user/" + userId;

            Map<String, Object> message = convertToWebSocketMessage(event);

            messagingTemplate.convertAndSend(destination, message);

            if (log.isDebugEnabled()) {
                log.debug("개인 메시지 전송 완료: userId={}, eventType={}",
                        userId, event.getEventType());
            }

        } catch (Exception e) {
            log.error("개인 메시지 전송 실패: userId={}, eventType={}, error={}",
                    userId, event.getEventType(), e.getMessage(), e);
        }
    }

    /**
     * 알림 전송
     */
    public void sendNotification(Long roomId, ChatEvent event) {
        try {
            String destination = "/topic/notifications/" + roomId;

            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NOTIFICATION");
            notification.put("eventType", event.getEventType().toString());
            notification.put("roomId", event.getChatRoomId());
            notification.put("userId", event.getUserId());
            notification.put("username", event.getUsername());
            notification.put("content", event.getContent());
            notification.put("timestamp", event.getTimestamp().toString());
            notification.put("eventId", event.getEventId());

            messagingTemplate.convertAndSend(destination, notification);

            if (log.isDebugEnabled()) {
                log.debug("알림 전송 완료: roomId={}, eventType={}", roomId, event.getEventType());
            }

        } catch (Exception e) {
            log.error("알림 전송 실패: roomId={}, eventType={}, error={}",
                    roomId, event.getEventType(), e.getMessage(), e);
        }
    }

    /**
     * ChatEvent를 WebSocket 메시지 포맷으로 변환
     */
    private Map<String, Object> convertToWebSocketMessage(ChatEvent event) {
        Map<String, Object> message = new HashMap<>();

        // 기본 정보
        message.put("eventId", event.getEventId());
        message.put("type", event.getEventType().toString());
        message.put("roomId", event.getChatRoomId());
        message.put("userId", event.getUserId());
        message.put("username", event.getUsername());
        message.put("timestamp", event.getTimestamp().toString());

        // 이벤트 타입별 특화 데이터
        switch (event.getEventType()) {
            case MESSAGE_SENT:
                message.put("messageId", event.getMessageId());
                message.put("content", event.getMessageContent());
                message.put("status", event.getMessageStatus());
                break;

            case USER_JOINED:
            case USER_LEFT:
            case ROOM_CREATED:
                message.put("content", event.getContent());
                break;

            default:
                if (event.getContent() != null) {
                    message.put("content", event.getContent());
                }
        }

        // 메타데이터가 있으면 포함
        if (event.getMetadata() != null && !event.getMetadata().isEmpty()) {
            message.put("metadata", event.getMetadata());
        }

        return message;
    }
}
