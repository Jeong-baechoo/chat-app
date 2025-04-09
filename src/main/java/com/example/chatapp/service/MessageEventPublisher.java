package com.example.chatapp.service;

import com.example.chatapp.domain.Message;
import com.example.chatapp.event.MessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 메시지 이벤트 발행자
 * 메시지 관련 이벤트 발행을 담당합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 메시지 생성 이벤트 발행
     * 
     * @param message 생성된 메시지
     */
    public void publishMessageCreatedEvent(Message message) {
        MessageCreatedEvent event = new MessageCreatedEvent(
                message.getId(),
                message.getSender().getId(),
                message.getChatRoom().getId(),
                message.getTimestamp()
        );
        eventPublisher.publishEvent(event);
        log.debug("메시지 생성 이벤트 발행: messageId={}", message.getId());
    }
}
