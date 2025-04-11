package com.example.chatapp.controller;

import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.exception.UserException;
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
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 메시지 전송
     */
    @MessageMapping("/message.send")
    public void sendMessage(@Payload MessageCreateRequest request) {
        log.debug("WebSocket 메시지 전송: senderId={}, roomId={}",
                request.getSenderId(), request.getChatRoomId());

        // 메시지 저장 및 DTO로 변환
        MessageResponse messageDTO = messageService.sendMessage(request);

        // 메시지 이벤트 전송
        broadcastMessage("MESSAGE", messageDTO.getSender(), request.getChatRoomId(),
                messageDTO.getContent(), messageDTO, null);

        log.debug("메시지 전송 성공: id={}", messageDTO.getId());
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

        // 입장 이벤트 전송
        String content = user.getUsername() + "님이 채팅방에 입장했습니다.";
        broadcastMessage("ENTER", user, roomId, content, null, null);

        log.info("사용자 입장: userId={}, roomId={}", userId, roomId);
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

        // 퇴장 이벤트 전송
        String content = user.getUsername() + "님이 채팅방에서 퇴장했습니다.";
        broadcastMessage("LEAVE", user, roomId, content, null, null);

        log.info("사용자 퇴장: userId={}, roomId={}", userId, roomId);
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
     * 공통 메시지 브로드캐스트 처리
     */
    private void broadcastMessage(String type, UserResponse sender, Long roomId, String content,
                                  MessageResponse messageData, Map<String, Object> additionalData) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("sender", sender);
        message.put("roomId", roomId);
        message.put("content", content);
        message.put("timestamp", LocalDateTime.now().toString());

        // 메시지 타입에 따른 추가 데이터 설정
        if (type.equals("MESSAGE") && messageData != null) {
            message.put("id", messageData.getId());
            message.put("status", messageData.getStatus().toString());
        }

        // 추가 데이터가 있으면 포함
        if (additionalData != null) {
            message.putAll(additionalData);
        }

        // 채팅방에 메시지 브로드캐스트
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }

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
