package com.example.chatapp.controller;

import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.infrastructure.kafka.ChatEventProducer;
import com.example.chatapp.infrastructure.message.ChatEvent;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {
    private final UserService userService;
    private final MessageService messageService;
    private final ChatEventProducer chatEventProducer;

    /**
     * 메시지 전송
     */
    @MessageMapping("/message.send")
    public void sendMessage(@Payload MessageCreateRequest request) {
        log.debug("WebSocket 메시지 전송 요청: senderId={}, roomId={}",
                request.getSenderId(), request.getChatRoomId());

        // 메시지 저장 (Kafka 이벤트는 MessageService에서 자동 발행)
        MessageResponse messageDTO = messageService.sendMessage(request);

        log.debug("메시지 전송 요청 처리 완료: id={}", messageDTO.getId());
    }

    /**
     * 채팅방 입장
     */
    @MessageMapping("/room.enter")
    public void enterRoom(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor){
        Long userId = extractLongValue(payload, "userId");
        Long roomId = extractLongValue(payload, "roomId");

        // 사용자 정보 조회
        UserResponse user = userService.findUserById(userId);

        // 세션에 사용자 정보 저장
        updateSessionAttributes(headerAccessor, userId, roomId);

        // 사용자 입장 이벤트를 Kafka로 발행
        ChatEvent userJoinEvent = ChatEvent.userJoinEvent(roomId, userId, user.getUsername());
        chatEventProducer.sendChatRoomEvent(userJoinEvent);

        log.info("사용자 입장 이벤트 발행: userId={}, roomId={}", userId, roomId);
    }

    /**
     * 채팅방 퇴장
     */
    @MessageMapping("/room.leave")
    public void leaveRoom(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = extractLongValue(payload, "userId");
        Long roomId = extractLongValue(payload, "roomId");

        // 사용자 정보 조회
        UserResponse user = userService.findUserById(userId);

        // 세션에서 사용자 정보 제거
        clearSessionAttributes(headerAccessor);

        // 사용자 퇴장 이벤트를 Kafka로 발행
        ChatEvent userLeaveEvent = ChatEvent.userLeaveEvent(roomId, userId, user.getUsername());
        chatEventProducer.sendChatRoomEvent(userLeaveEvent);

        log.info("사용자 퇴장 이벤트 발행: userId={}, roomId={}", userId, roomId);
    }

    /**
     * WebSocket 예외 처리
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, Object> handleException(Exception e) {
        Map<String, Object> errorMessage = createErrorMessage(e);
        log.error("WebSocket 오류: {}", e.getMessage(), e);
        return errorMessage;
    }

    //---------------- 유틸리티 메서드 ----------------//

    /**
     * 세션 속성 업데이트
     */
    private void updateSessionAttributes(SimpMessageHeaderAccessor headerAccessor, Long userId, Long roomId) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("userId", userId);
            sessionAttributes.put("roomId", roomId);
        }
    }

    /**
     * 세션 속성 제거
     */
    private void clearSessionAttributes(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.remove("userId");
            sessionAttributes.remove("roomId");
        }
    }

    /**
     * 오류 메시지 생성
     */
    private Map<String, Object> createErrorMessage(Exception e) {
        Map<String, Object> errorMessage = new HashMap<>();
        errorMessage.put("timestamp", LocalDateTime.now().toString());

        String status = "SERVER_ERROR";
        if (e instanceof UserException) {
            status = "USER_ERROR";
        } else if (e instanceof ChatRoomException) {
            status = "CHATROOM_ERROR";
        } else if (e instanceof MessageException) {
            status = "MESSAGE_ERROR";
        }

        errorMessage.put("status", status);
        errorMessage.put("message", e.getMessage());

        return errorMessage;
    }

    /**
     * Map에서 Long 값 추출
     */
    private Long extractLongValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            throw new IllegalArgumentException(key + " 값이 필요합니다.");
        }
        return Long.valueOf(value.toString());
    }

}
