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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 채팅 분석 및 통계를 담당하는 Consumer
 * 사용자 활동, 메시지 통계, 채팅방 사용률 등을 분석
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsConsumer {

//    @KafkaListener(
//        topics = {KafkaConfig.CHAT_MESSAGES_TOPIC, KafkaConfig.CHAT_EVENTS_TOPIC},
//        groupId = "analytics-group",
//        containerFactory = "kafkaListenerContainerFactory"
//    )
    public void handleAnalyticsEvent(
            @Payload ChatEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        try {
            log.debug("분석 이벤트 수신: eventType={}, topic={}, roomId={}",
                    event.getEventType(), topic, event.getChatRoomId());

            switch (event.getEventType()) {
                case MESSAGE_SENT:
                    analyzeMessage(event);
                    break;
                case USER_JOINED:
                    analyzeUserJoin(event);
                    break;
                case USER_LEFT:
                    analyzeUserLeave(event);
                    break;
                case ROOM_CREATED:
                    analyzeRoomCreation(event);
                    break;
                default:
                    log.debug("분석하지 않는 이벤트 타입: {}", event.getEventType());
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("분석 처리 실패: eventType={}, roomId={}, error={}",
                    event.getEventType(), event.getChatRoomId(), e.getMessage(), e);
            ack.acknowledge(); // 실패해도 ack하여 무한 재시도 방지
        }
    }

    private void analyzeMessage(ChatEvent event) {
        // 메시지 통계 분석
        String hour = event.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));

        log.info("메시지 분석: 시간={}, 룸={}, 사용자={}, 길이={}",
                hour,
                event.getChatRoomId(),
                event.getUserId(),
                event.getMessageContent() != null ? event.getMessageContent().length() : 0);

        // TODO: 실제 구현에서는 다음과 같은 메트릭을 수집
        // - 시간대별 메시지 수
        // - 채팅방별 활성도
        // - 사용자별 메시지 수
        // - 평균 메시지 길이
        // - 인기 키워드 분석

        // 예: Redis나 InfluxDB에 메트릭 저장
        // metricsService.incrementMessageCount(hour, event.getChatRoomId());
        // metricsService.recordMessageLength(event.getMessageContent().length());
    }

    private void analyzeUserJoin(ChatEvent event) {
        // 사용자 입장 분석
        log.info("사용자 입장 분석: 룸={}, 사용자={}, 시간={}",
                event.getChatRoomId(),
                event.getUserId(),
                event.getTimestamp());

        // TODO: 입장 패턴 분석
        // - 시간대별 입장자 수
        // - 채팅방별 인기도
        // - 사용자별 활동 패턴
    }

    private void analyzeUserLeave(ChatEvent event) {
        // 사용자 퇴장 분석
        log.info("사용자 퇴장 분석: 룸={}, 사용자={}, 시간={}",
                event.getChatRoomId(),
                event.getUserId(),
                event.getTimestamp());

        // TODO: 퇴장 패턴 분석
        // - 평균 체류 시간
        // - 이탈률 분석
    }

    private void analyzeRoomCreation(ChatEvent event) {
        // 채팅방 생성 분석
        log.info("채팅방 생성 분석: 룸={}, 생성자={}, 시간={}",
                event.getChatRoomId(),
                event.getUserId(),
                event.getTimestamp());

        // TODO: 채팅방 생성 패턴 분석
        // - 시간대별 생성 수
        // - 사용자별 생성 패턴
    }
}
