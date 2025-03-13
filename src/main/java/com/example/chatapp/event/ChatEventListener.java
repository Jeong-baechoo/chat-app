package com.example.chatapp.event;

import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    @EventListener
    public void handleMessageCreatedEvent(MessageCreatedEvent event) {
        log.debug("메시지 생성 이벤트 수신: messageId={}, chatRoomId={}",
                event.getMessageId(), event.getChatRoomId());

        try {
            // 메시지 상세 정보 조회
            MessageResponse message = messageService.findMessageById(event.getMessageId());

            // 실시간 알림 전송
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "NEW_MESSAGE");
            notification.put("messageId", event.getMessageId());
            notification.put("chatRoomId", event.getChatRoomId());
            notification.put("senderId", event.getSenderId());
            notification.put("timestamp", event.getTimestamp().toString());

            // 채팅방 구독자들에게 알림 전송
            messagingTemplate.convertAndSend(
                    "/topic/notifications/" + event.getChatRoomId(),
                    notification
            );

            log.debug("알림 전송 완료: chatRoomId={}", event.getChatRoomId());
        } catch (Exception e) {
            log.error("메시지 알림 전송 실패: {}", e.getMessage(), e);
        }
    }
}
