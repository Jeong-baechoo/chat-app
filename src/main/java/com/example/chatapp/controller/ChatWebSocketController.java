package com.example.chatapp.controller;

import com.example.chatapp.domain.User;
import com.example.chatapp.dto.MessageDTO;
import com.example.chatapp.dto.MessageRequestDTO;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.repository.UserRepository;
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
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    /**
     * 메시지 전송 처리
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageRequestDTO requestDTO) {
        log.debug("메시지 전송 요청: {}", requestDTO);

        try {
            // 메시지 저장 및 DTO로 변환
            MessageDTO messageDTO = messageService.sendMessage(requestDTO);

            // 브로드캐스트용 메시지 생성
            Map<String, Object> broadcastMessage = new HashMap<>();
            broadcastMessage.put("id", messageDTO.getId());
            broadcastMessage.put("content", messageDTO.getContent());
            broadcastMessage.put("sender", messageDTO.getSender());
            broadcastMessage.put("chatRoomId", messageDTO.getChatRoomId());
            broadcastMessage.put("status", messageDTO.getStatus().toString());
            broadcastMessage.put("timestamp", messageDTO.getTimestamp().toString());

            // 채팅방에 메시지 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + requestDTO.getChatRoomId(),
                    broadcastMessage
            );

            log.debug("메시지 전송 성공: {}", messageDTO.getId());
        } catch (Exception e) {
            log.error("메시지 전송 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 사용자 입장 처리
     */
    @MessageMapping("/chat.addUser")
    public void addUser(
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            Long chatRoomId = Long.valueOf(payload.get("chatRoomId").toString());

            // 사용자 정보 조회
            Optional<User> user = userService.findUserById(userId);

            // 세션에 사용자 정보 저장
            headerAccessor.getSessionAttributes().put("userId", userId);
            headerAccessor.getSessionAttributes().put("chatRoomId", chatRoomId);

            // 브로드캐스트용 메시지 생성
            Map<String, Object> joinMessage = new HashMap<>();
            joinMessage.put("type", "JOIN");
            joinMessage.put("sender", user);
            joinMessage.put("chatRoomId", chatRoomId);
            joinMessage.put("timestamp", LocalDateTime.now().toString());

            // 채팅방에 입장 메시지 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId,
                    joinMessage
            );

            log.info("사용자 입장: userId={}, chatRoomId={}", userId, chatRoomId);
        } catch (Exception e) {
            log.error("사용자 입장 처리 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 사용자 퇴장 처리
     */
    @MessageMapping("/chat.leaveUser")
    public void leaveUser(
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            Long chatRoomId = Long.valueOf(payload.get("chatRoomId").toString());

            // 사용자 정보 조회
            Optional<User> user = userService.findUserById(userId);

            // 세션에서 사용자 정보 제거
            headerAccessor.getSessionAttributes().remove("userId");
            headerAccessor.getSessionAttributes().remove("chatRoomId");

            // 브로드캐스트용 메시지 생성
            Map<String, Object> leaveMessage = new HashMap<>();
            leaveMessage.put("type", "LEAVE");
            leaveMessage.put("sender", user);
            leaveMessage.put("chatRoomId", chatRoomId);
            leaveMessage.put("timestamp", LocalDateTime.now().toString());

            // 채팅방에 퇴장 메시지 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chatRoomId,
                    leaveMessage
            );

            log.info("사용자 퇴장: userId={}, chatRoomId={}", userId, chatRoomId);
        } catch (Exception e) {
            log.error("사용자 퇴장 처리 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 메시지 오류 처리
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, Object> handleException(Exception e) {
        Map<String, Object> errorMessage = new HashMap<>();
        errorMessage.put("timestamp", LocalDateTime.now().toString());

        if (e instanceof UserException) {
            errorMessage.put("status", "USER_ERROR");
        } else if (e instanceof ChatRoomException) {
            errorMessage.put("status", "CHATROOM_ERROR");
        } else if (e instanceof MessageException) {
            errorMessage.put("status", "MESSAGE_ERROR");
        } else {
            errorMessage.put("status", "SERVER_ERROR");
        }

        errorMessage.put("message", e.getMessage());

        log.error("WebSocket 오류: {}", e.getMessage(), e);
        return errorMessage;
    }
}
