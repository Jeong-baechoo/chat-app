package com.example.chatapp.infrastructure.kafka.consumer;

import com.example.chatapp.config.KafkaConfig;
import com.example.chatapp.domain.Message;
import com.example.chatapp.infrastructure.message.ChatEvent;
import com.example.chatapp.infrastructure.message.ChatEventType;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.service.EntityFinderService;
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
import java.util.Set;

/**
 * 메시지 영구 저장을 담당하는 Consumer
 * 별도 그룹으로 분리하여 저장과 전송을 독립적으로 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessagePersistenceConsumer {

    private final MessageRepository messageRepository;
    private final EntityFinderService entityFinderService;

    @KafkaListener(
        topics = KafkaConfig.CHAT_MESSAGES_TOPIC,
        groupId = "message-batch-persistence-group",
        containerFactory = "batchKafkaListenerContainerFactory",
        batch = "true"
    )
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

                // 데이터베이스에서 존재하는 메시지 확인
                List<Message> existingMessages = messageRepository.findAllById(messageIds);

                // 존재하는 메시지 ID 집합
                Set<Long> existingIds = existingMessages.stream()
                        .map(Message::getId)
                        .collect(Collectors.toSet());

                // 업데이트가 필요한 메시지 처리
                // 예: 읽음 상태 업데이트 등
                List<Message> messagesToUpdate = existingMessages.stream()
                        .filter(msg -> messageEvents.stream()
                                .anyMatch(event -> event.getMessageId().equals(msg.getId()) &&
                                         event.getMessageStatus() != null &&
                                         !event.getMessageStatus().equals(msg.getStatus().name())))
                        .collect(Collectors.toList());

                if (!messagesToUpdate.isEmpty()) {
                    // 상태 업데이트가 필요한 메시지 일괄 업데이트
                    messageRepository.saveAll(messagesToUpdate);
                    log.debug("메시지 상태 배치 업데이트 완료: {}개", messagesToUpdate.size());
                }

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
