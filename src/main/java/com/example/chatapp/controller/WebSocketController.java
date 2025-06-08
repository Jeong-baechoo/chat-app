package com.example.chatapp.controller;

import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.request.RoomEnterRequest;
import com.example.chatapp.dto.request.RoomLeaveRequest;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {
    private final MessageService messageService;
    private final ChatRoomService chatRoomService;

    /**
     * 메시지 전송
     */
    @MessageMapping("/message.send")
    public void sendMessage(@Payload @Valid MessageCreateRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long senderId = getUserIdFromSession(headerAccessor);

        log.debug("WebSocket 메시지 전송 요청: senderId={}, roomId={}",
                senderId, request.getChatRoomId());

        messageService.sendMessage(request, senderId);

        log.debug("메시지 전송 요청 처리 완료");
    }

    /**
     * 채팅방 입장
     */
    @MessageMapping("/room.enter")
    public void enterRoom(@Payload @Valid RoomEnterRequest request, SimpMessageHeaderAccessor headerAccessor){
        // WebSocket 세션에서 인증된 사용자 ID 추출
        Long userId = getUserIdFromSession(headerAccessor);
        Long roomId = request.getRoomId();

        // 채팅방 참가자로 추가 (실제 비즈니스 로직)
        chatRoomService.addParticipantToChatRoom(roomId, userId);

        // 세션에 사용자 정보 저장
        updateSessionAttributes(headerAccessor, userId, roomId);

        log.info("사용자 채팅방 입장 완료: userId={}, roomId={}", userId, roomId);
    }

    /**
     * 채팅방 퇴장
     */
    @MessageMapping("/room.leave")
    public void leaveRoom(@Payload @Valid RoomLeaveRequest request, SimpMessageHeaderAccessor headerAccessor) {
        // WebSocket 세션에서 인증된 사용자 ID 추출
        Long userId = getUserIdFromSession(headerAccessor);
        Long roomId = request.getRoomId();

        // 채팅방에서 참가자 제거 (실제 비즈니스 로직)
        chatRoomService.removeParticipantFromChatRoom(roomId, userId);

        // 현재 채팅방에서만 퇴장 (userId는 보존, roomId만 제거)
        removeCurrentRoomFromSession(headerAccessor);

        log.info("사용자 채팅방 퇴장 완료: userId={}, roomId={}", userId, roomId);
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
     * 현재 채팅방 정보만 세션에서 제거 (userId는 보존)
     */
    private void removeCurrentRoomFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            // userId는 WebSocket 연결 동안 유지되어야 하므로 제거하지 않음
            sessionAttributes.remove("roomId");
        }
    }

    /**
     * 오류 메시지 생성
     */
    private Map<String, Object> createErrorMessage(Exception e) {
        String status = determineErrorStatus(e);

        return Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "status", status,
            "message", e.getMessage()
        );
    }

    /**
     * 예외 타입에 따른 상태 코드 결정
     */
    private String determineErrorStatus(Exception e) {
        if (e instanceof UserException) {
            return "USER_ERROR";
        } else if (e instanceof ChatRoomException) {
            return "CHATROOM_ERROR";
        } else if (e instanceof MessageException) {
            return "MESSAGE_ERROR";
        }
        return "SERVER_ERROR";
    }


    /**
     * 세션에서 사용자 ID 가져오기
     */
    private Long getUserIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        Object userId = sessionAttributes.get("userId");
        if (userId == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        return Long.valueOf(userId.toString());
    }

}
