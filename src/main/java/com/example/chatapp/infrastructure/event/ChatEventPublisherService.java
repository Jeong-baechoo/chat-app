package com.example.chatapp.infrastructure.event;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.Message;
import com.example.chatapp.domain.User;
import com.example.chatapp.event.MessageCreatedEvent;
import com.example.chatapp.infrastructure.kafka.ChatEventProducer;
import com.example.chatapp.infrastructure.message.ChatEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 채팅 관련 이벤트 발행을 담당하는 서비스
 * 비즈니스 로직과 인프라 로직(이벤트 발행)을 분리하기 위한 클래스
 *
 * 두 가지 이벤트 발행 메커니즘을 지원:
 * 1. Kafka를 통한 외부 이벤트 발행 (ChatEventProducer 사용)
 * 2. Spring ApplicationEventPublisher를 통한 내부 이벤트 발행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatEventPublisherService {

    private final ChatEventProducer chatEventProducer;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 채팅방 생성 이벤트 발행
     */
    @Async
    public void publishRoomCreatedEvent(ChatRoom chatRoom, User creator) {
        try {
            // 1. Kafka 이벤트 발행
            ChatEvent roomCreatedEvent = ChatEvent.roomCreatedEvent(
                chatRoom.getId(),
                    String.valueOf(chatRoom.getName()),
                creator.getId(),
                creator.getUsername()
            );

            chatEventProducer.sendChatRoomEvent(roomCreatedEvent);
            chatEventProducer.sendNotificationEvent(roomCreatedEvent);

            // 2. 내부 이벤트는 필요 시 여기에 추가 가능

            log.debug("채팅방 생성 이벤트 발행 완료: roomId={}, creatorId={}",
                    chatRoom.getId(), creator.getId());
        } catch (Exception e) {
            log.error("채팅방 생성 이벤트 발행 실패: roomId={}, creatorId={}, error={}",
                    chatRoom.getId(), creator.getId(), e.getMessage(), e);
        }
    }

    /**
     * 사용자 입장 이벤트 발행
     */
    @Async
    public void publishUserJoinEvent(Long chatRoomId, User user) {
        try {
            // 1. Kafka 이벤트 발행
            ChatEvent userJoinEvent = ChatEvent.userJoinEvent(
                chatRoomId,
                user.getId(),
                user.getUsername()
            );

            chatEventProducer.sendChatRoomEvent(userJoinEvent);
            // 주석 처리된 코드는 의도적인 것으로 보이므로 그대로 유지
            // chatEventProducer.sendNotificationEvent(userJoinEvent);

            // 2. 내부 이벤트는 필요 시 여기에 추가 가능

            log.debug("사용자 입장 이벤트 발행 완료: roomId={}, userId={}",
                    chatRoomId, user.getId());
        } catch (Exception e) {
            log.error("사용자 입장 이벤트 발행 실패: roomId={}, userId={}, error={}",
                    chatRoomId, user.getId(), e.getMessage(), e);
        }
    }

    /**
     * 메시지 전송 이벤트 발행
     * Kafka 이벤트와 Spring 내부 이벤트 모두 발행
     */
    @Async
    public void publishMessageEvent(Message message, User sender) {
        try {
            // 1. Kafka 이벤트 발행
            ChatEvent messageEvent = ChatEvent.messageEvent(
                message.getId(),
                message.getContent(),
                message.getChatRoom().getId(),
                sender.getId(),
                sender.getUsername()
            );

            chatEventProducer.sendMessageEvent(messageEvent);

            // 2. Spring 내부 이벤트 발행 (MessageCreatedEvent)
            MessageCreatedEvent internalEvent = new MessageCreatedEvent(
                message.getId(),
                sender.getId(),
                message.getChatRoom().getId(),
                message.getTimestamp()
            );
            applicationEventPublisher.publishEvent(internalEvent);

            log.debug("메시지 이벤트 발행 완료: messageId={}, chatRoomId={}",
                    message.getId(), message.getChatRoom().getId());
        } catch (Exception e) {
            log.error("메시지 이벤트 발행 실패: messageId={}, error={}",
                    message.getId(), e.getMessage(), e);
        }
    }
}
