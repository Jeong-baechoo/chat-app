package com.example.chatapp.infrastructure.kafka.serialization;

import com.example.chatapp.infrastructure.message.ChatEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

@Slf4j
public class ChatEventDeserializer implements Deserializer<ChatEvent> {

    private final ObjectMapper objectMapper;

    public ChatEventDeserializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        // 알 수 없는 속성 무시
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // 필요한 설정이 있으면 여기서 처리
    }

    @Override
    public ChatEvent deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            // JSON 로그 출력 제거 (프로덕션 환경에서 로그 양 감소)
            return objectMapper.readValue(data, ChatEvent.class);
        } catch (Exception e) {
            log.error("ChatEvent 역직렬화 실패: {}", e.getMessage(), e);
            // 실패 시 null 반환 (무한 재시도 방지)
            return null;
        }
    }

    @Override
    public void close() {
        // 리소스 정리가 필요하면 여기서 처리
    }
}
