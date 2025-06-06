package com.example.chatapp.infrastructure.kafka.consumer;

import com.example.chatapp.config.KafkaConfig;
import com.example.chatapp.infrastructure.message.ChatEvent;
import com.example.chatapp.infrastructure.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 실시간 메시지 전달을 담당하는 Consumer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageDeliveryConsumer {

    private final WebSocketService webSocketService;

    /**
     * 개별 메시지 처리 리스너
     */
    @KafkaListener(
        topics = KafkaConfig.CHAT_MESSAGES_TOPIC,
        groupId = "message-delivery-group-v2",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMessageEvent(
            @Payload ChatEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        try {
            log.debug("메시지 이벤트 수신: eventType={}, roomId={}, eventId={}",
                    event.getEventType(), event.getChatRoomId(), event.getEventId());

            // WebSocket을 통해 채팅방 참여자들에게 실시간 전송
            webSocketService.broadcastToRoom(event.getChatRoomId(), event);

            // 수동 커밋
            ack.acknowledge();

            log.debug("메시지 전달 완료: eventType={}, roomId={}",
                    event.getEventType(), event.getChatRoomId());

        } catch (Exception e) {
            log.error("메시지 전달 실패: eventType={}, roomId={}, error={}",
                    event.getEventType(), event.getChatRoomId(), e.getMessage(), e);

            // 실패 시에도 acknowledge하여 무한 재시도 방지
            // 실제 운영에서는 DLQ(Dead Letter Queue) 사용 고려
            ack.acknowledge();
        }
    }

    /**
     * 배치 메시지 처리 리스너
     */
//    @KafkaListener(
//        topics = KafkaConfig.CHAT_MESSAGES_TOPIC,
//        groupId = "message-batch-delivery-group",
//        containerFactory = "batchKafkaListenerContainerFactory",
//        batch = "true"
//    )
    public void handleMessageEventsBatch(
            @Payload List<ChatEvent> events,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {

        try {
            log.debug("메시지 이벤트 배치 수신: 총 {}개 이벤트", events.size());

            // 채팅방별로 이벤트 그룹화
            Map<Long, List<ChatEvent>> roomEvents = events.stream()
                    .collect(Collectors.groupingBy(ChatEvent::getChatRoomId));

            // 각 채팅방별로 한 번에 처리
            for (Map.Entry<Long, List<ChatEvent>> entry : roomEvents.entrySet()) {
                Long roomId = entry.getKey();
                List<ChatEvent> roomMessages = entry.getValue();

                // WebSocket을 통해 채팅방 참여자들에게 실시간 전송 (배치)
                webSocketService.broadcastBatchToRoom(roomId, roomMessages);

                log.debug("채팅방 {} 메시지 배치 전달 완료: {}개 메시지",
                        roomId, roomMessages.size());
            }

            // 수동 커밋
            ack.acknowledge();

        } catch (Exception e) {
            log.error("메시지 배치 전달 실패: events={}, error={}",
                    events.size(), e.getMessage(), e);
            ack.acknowledge();
        }
    }

    @KafkaListener(
        topics = KafkaConfig.CHAT_EVENTS_TOPIC,
        groupId = "room-events-delivery-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRoomEvent(
            @Payload ChatEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {

        try {
            log.debug("룸 이벤트 수신: eventType={}, roomId={}, eventId={}",
                    event.getEventType(), event.getChatRoomId(), event.getEventId());

            // 채팅방 이벤트도 실시간으로 전송 (입장/퇴장 등)
            webSocketService.broadcastToRoom(event.getChatRoomId(), event);

            ack.acknowledge();

            log.debug("룸 이벤트 전달 완료: eventType={}, roomId={}",
                    event.getEventType(), event.getChatRoomId());

        } catch (Exception e) {
            log.error("룸 이벤트 전달 실패: eventType={}, roomId={}, error={}",
                    event.getEventType(), event.getChatRoomId(), e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
