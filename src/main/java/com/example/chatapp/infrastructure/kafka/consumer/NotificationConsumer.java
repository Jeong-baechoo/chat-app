package com.example.chatapp.infrastructure.kafka.consumer;

import com.example.chatapp.config.KafkaConfig;
import com.example.chatapp.infrastructure.message.ChatEvent;
import com.example.chatapp.infrastructure.message.ChatEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * 알림 처리를 담당하는 Consumer
 * 푸시 알림, 이메일 등의 알림 서비스 연동
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    @KafkaListener(
        topics = KafkaConfig.CHAT_NOTIFICATIONS_TOPIC,
        groupId = "notification-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleNotification(
            @Payload ChatEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {

        try {
            if (log.isDebugEnabled()) {
                log.debug("알림 이벤트 수신: eventType={}, userId={}",
                        event.getEventType(), event.getUserId());
            }

            switch (event.getEventType()) {
                case MESSAGE_SENT:
                    handleMessageNotification(event);
                    break;
                case USER_JOINED:
                    handleUserJoinNotification(event);
                    break;
                case USER_LEFT:
                    handleUserLeaveNotification(event);
                    break;
                case ROOM_CREATED:
                    handleRoomCreatedNotification(event);
                    break;
                default:
                    if (log.isDebugEnabled()) {
                        log.debug("처리하지 않는 알림 타입: {}", event.getEventType());
                    }
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("알림 처리 실패: eventType={}, userId={}, error={}",
                    event.getEventType(), event.getUserId(), e.getMessage(), e);
            ack.acknowledge();
        }
    }

    private void handleMessageNotification(ChatEvent event) {
        // 새 메시지 알림 처리
        // 실제 구현에서는 Firebase, APNs 등을 통한 푸시 알림 발송
        log.info("새 메시지 알림: roomId={}, sender={}, content={}",
                event.getChatRoomId(), event.getUsername(),
                event.getMessageContent() != null ?
                    event.getMessageContent().substring(0, Math.min(50, event.getMessageContent().length())) : "");

        // TODO: 실제 푸시 알림 서비스 연동
        // pushNotificationService.sendNewMessageNotification(event);
    }

    private void handleUserJoinNotification(ChatEvent event) {
        // 사용자 입장 알림 처리
        log.info("사용자 입장 알림: roomId={}, user={}",
                event.getChatRoomId(), event.getUsername());

        // TODO: 필요시 입장 알림 발송
    }

    private void handleUserLeaveNotification(ChatEvent event) {
        // 사용자 퇴장 알림 처리
        log.info("사용자 퇴장 알림: roomId={}, user={}",
                event.getChatRoomId(), event.getUsername());

        // TODO: 필요시 퇴장 알림 발송
    }

    private void handleRoomCreatedNotification(ChatEvent event) {
        // 채팅방 생성 알림 처리
        log.info("채팅방 생성 알림: roomId={}, creator={}",
                event.getChatRoomId(), event.getUsername());

        // TODO: 채팅방 생성 알림 발송
    }
}
