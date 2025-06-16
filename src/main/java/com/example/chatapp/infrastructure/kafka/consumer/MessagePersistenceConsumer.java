package com.example.chatapp.infrastructure.kafka.consumer;

import com.example.chatapp.config.KafkaConfig;
import com.example.chatapp.domain.Message;
import com.example.chatapp.infrastructure.message.ChatEvent;
import com.example.chatapp.infrastructure.message.ChatEventType;
import com.example.chatapp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 메시지 영구 저장을 담당하는 Consumer
 * 별도 그룹으로 분리하여 저장과 전송을 독립적으로 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessagePersistenceConsumer {

    private final MessageRepository messageRepository;

//    @KafkaListener(
//        topics = KafkaConfig.CHAT_MESSAGES_TOPIC,
//        groupId = "message-persistence-group",
//        containerFactory = "kafkaListenerContainerFactory"
//    )
    @Transactional
    public void persistMessage(
            @Payload ChatEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        try {
            if (log.isDebugEnabled()) {
                log.debug("메시지 저장 이벤트 수신: eventType={}, messageId={}",
                        event.getEventType(), event.getMessageId());
            }

            // 메시지 관련 이벤트만 처리
            if (event.getEventType() == ChatEventType.MESSAGE_SENT && event.getMessageId() != null) {
                // 이미 저장된 메시지인지 확인 (중복 방지)
                if (!messageRepository.existsById(event.getMessageId())) {
                    log.warn("메시지 ID {}가 데이터베이스에 없습니다. 이미 처리되었거나 잘못된 이벤트일 수 있습니다.",
                            event.getMessageId());
                }
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("메시지 저장 처리 실패: eventType={}, messageId={}, error={}",
                    event.getEventType(), event.getMessageId(), e.getMessage(), e);
            ack.acknowledge(); // 실패해도 ack하여 무한 재시도 방지
        }
    }

//    @KafkaListener(
//        topics = KafkaConfig.CHAT_MESSAGES_TOPIC,
//        groupId = "message-batch-persistence-group",
//        containerFactory = "batchKafkaListenerContainerFactory",
//        batch = "true"
//    )
    @Transactional
    public void persistMessageBatch(
            @Payload List<ChatEvent> events,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment ack) {

        try {
            if (log.isDebugEnabled()) {
                log.debug("메시지 저장 배치 이벤트 수신: 총 {}개 이벤트", events.size());
            }

            // 처리할 메시지 이벤트만 필터링
            List<ChatEvent> messageEvents = events.stream()
                    .filter(event -> event.getEventType() == ChatEventType.MESSAGE_SENT
                            && event.getMessageId() != null)
                    .collect(Collectors.toList());

            if (!messageEvents.isEmpty()) {
                // 메시지 ID 목록 추출
                List<Long> messageIds = messageEvents.stream()
                        .map(ChatEvent::getMessageId)
                        .collect(Collectors.toList());

                // 실제 운영에서는 배치 처리 로직 구현
                // 예: 메시지 읽음 상태 업데이트, 검색 인덱스 업데이트 등

                if (log.isDebugEnabled()) {
                    log.debug("메시지 배치 처리 완료: {}개 메시지", messageIds.size());
                }
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("메시지 배치 저장 처리 실패: events={}, error={}",
                    events.size(), e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
