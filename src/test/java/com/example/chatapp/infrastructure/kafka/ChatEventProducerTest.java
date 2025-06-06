package com.example.chatapp.infrastructure.kafka;

import com.example.chatapp.infrastructure.message.ChatEvent;
import com.example.chatapp.infrastructure.message.ChatEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatEventProducerTest {

    @Mock
    private KafkaTemplate<String, ChatEvent> kafkaTemplate;

    @InjectMocks
    private ChatEventProducer chatEventProducer;

    @Test
    @DisplayName("메시지 이벤트 발송 성공")
    void givenMessageEvent_whenSendMessageEvent_thenEventSent() {
        // Given
        ChatEvent event = ChatEvent.builder()
                .eventId("test-event-id")
                .eventType(ChatEventType.MESSAGE_SENT)
                .chatRoomId(1L)
                .userId(1L)
                .username("testuser")
                .messageId(1L)
                .messageContent("테스트 메시지")
                .timestamp(LocalDateTime.now())
                .build();

        CompletableFuture<SendResult<String, ChatEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(ChatEvent.class))).thenReturn(future);

        // When
        chatEventProducer.sendMessageEvent(event);

        // Then
        verify(kafkaTemplate).send(eq("chat-messages"), eq("room-1"), eq(event));
    }

    @Test
    @DisplayName("채팅방 이벤트 발송 성공")
    void givenRoomEvent_whenSendChatRoomEvent_thenEventSent() {
        // Given
        ChatEvent event = ChatEvent.builder()
                .eventId("test-event-id")
                .eventType(ChatEventType.USER_JOINED)
                .chatRoomId(1L)
                .userId(1L)
                .username("testuser")
                .content("testuser님이 채팅방에 입장했습니다.")
                .timestamp(LocalDateTime.now())
                .build();

        CompletableFuture<SendResult<String, ChatEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(ChatEvent.class))).thenReturn(future);

        // When
        chatEventProducer.sendChatRoomEvent(event);

        // Then
        verify(kafkaTemplate).send(eq("chat-events"), eq("room-1"), eq(event));
    }

    @Test
    @DisplayName("알림 이벤트 발송 성공")
    void givenNotificationEvent_whenSendNotificationEvent_thenEventSent() {
        // Given
        ChatEvent event = ChatEvent.builder()
                .eventId("test-event-id")
                .eventType(ChatEventType.MESSAGE_SENT)
                .chatRoomId(1L)
                .userId(2L)
                .username("sender")
                .messageContent("새 메시지")
                .timestamp(LocalDateTime.now())
                .build();

        CompletableFuture<SendResult<String, ChatEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(ChatEvent.class))).thenReturn(future);

        // When
        chatEventProducer.sendNotificationEvent(event);

        // Then
        verify(kafkaTemplate).send(eq("chat-notifications"), eq("user-2"), eq(event));
    }
}
