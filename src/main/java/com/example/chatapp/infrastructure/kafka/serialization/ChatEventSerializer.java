package com.example.chatapp.infrastructure.kafka.serialization;

import com.example.chatapp.infrastructure.message.ChatEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

@Slf4j
public class ChatEventSerializer implements Serializer<ChatEvent> {

    private final ObjectMapper objectMapper;

    public ChatEventSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // 필요한 설정이 있으면 여기서 처리
    }

    @Override
    public byte[] serialize(String topic, ChatEvent data) {
        if (data == null) {
            return null;
        }

        try {
            byte[] serialized = objectMapper.writeValueAsBytes(data);
            // 직렬화 결과 로그 제거 (프로덕션 환경에서 로그 양 감소)
            return serialized;
        } catch (Exception e) {
            log.error("ChatEvent 직렬화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("ChatEvent 직렬화 실패", e);
        }
    }

    @Override
    public void close() {
        // 리소스 정리가 필요하면 여기서 처리
    }
}
