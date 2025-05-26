package com.example.chatapp.infrastructure.kafka;

import com.example.chatapp.config.KafkaConfig;
import com.example.chatapp.infrastructure.message.ChatEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatEventProducer {

    private final KafkaTemplate<String, ChatEvent> kafkaTemplate;

    /**
     * 채팅 메시지 이벤트 발행
     */
    public void sendMessageEvent(ChatEvent event) {
        String key = generateMessageKey(event);
        sendEvent(KafkaConfig.CHAT_MESSAGES_TOPIC, key, event);
    }

    /**
     * 채팅방 이벤트 발행 (입장/퇴장/생성 등)
     */
    public void sendChatRoomEvent(ChatEvent event) {
        String key = generateRoomKey(event);
        sendEvent(KafkaConfig.CHAT_EVENTS_TOPIC, key, event);
    }

    /**
     * 알림 이벤트 발행
     */
    public void sendNotificationEvent(ChatEvent event) {
        String key = generateNotificationKey(event);
        sendEvent(KafkaConfig.CHAT_NOTIFICATIONS_TOPIC, key, event);
    }

    /**
     * 메시지 이벤트 배치 발행
     */
    public void sendMessageEventsBatch(List<ChatEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        // 채팅방별로 이벤트 그룹화 (같은 키로 발행되도록)
        Map<Long, List<ChatEvent>> roomEvents = events.stream()
                .collect(Collectors.groupingBy(ChatEvent::getChatRoomId));

        List<CompletableFuture<SendResult<String, ChatEvent>>> futures = new ArrayList<>();

        for (Map.Entry<Long, List<ChatEvent>> entry : roomEvents.entrySet()) {
            Long roomId = entry.getKey();
            List<ChatEvent> roomMessages = entry.getValue();

            for (ChatEvent event : roomMessages) {
                String key = "room-" + roomId;
                futures.add(kafkaTemplate.send(KafkaConfig.CHAT_MESSAGES_TOPIC, key, event));
            }
        }

        // 모든 전송 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("배치 이벤트 발송 성공: 총 {}개 이벤트", events.size());
                    }
                } else {
                    log.error("배치 이벤트 발송 실패: {}", ex.getMessage(), ex);
                }
            });
    }

    /**
     * 공통 이벤트 발송 메서드
     */
    private void sendEvent(String topic, String key, ChatEvent event) {
        try {
            CompletableFuture<SendResult<String, ChatEvent>> future =
                kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("이벤트 발송 성공: topic={}, key={}, eventType={}, eventId={}",
                                topic, key, event.getEventType(), event.getEventId());
                    }
                } else {
                    log.error("이벤트 발송 실패: topic={}, key={}, eventType={}, error={}",
                            topic, key, event.getEventType(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("이벤트 발송 중 예외 발생: topic={}, key={}, eventType={}, error={}",
                    topic, key, event.getEventType(), e.getMessage(), e);
        }
    }

    /**
     * 메시지 키 생성 (같은 채팅방 메시지는 같은 파티션으로)
     */
    private String generateMessageKey(ChatEvent event) {
        return "room-" + event.getChatRoomId();
    }

    /**
     * 룸 이벤트 키 생성
     */
    private String generateRoomKey(ChatEvent event) {
        return "room-" + event.getChatRoomId();
    }

    /**
     * 알림 키 생성
     */
    private String generateNotificationKey(ChatEvent event) {
        return "user-" + event.getUserId();
    }
}
