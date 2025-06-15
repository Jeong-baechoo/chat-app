package com.example.chatapp.controller;

import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.request.RoomEnterRequest;
import com.example.chatapp.dto.request.RoomLeaveRequest;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "웹소켓", description = "실시간 메시징을 위한 웹소켓 엔드포인트")
public class WebSocketController {
    private final MessageService messageService;
    private final ChatRoomService chatRoomService;

    /**
     * 메시지 전송
     */
    @MessageMapping("/message.send")
    @Operation(summary = "메시지 전송", description = "웹소켓을 통해 채팅방에 메시지를 전송합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 전송 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 메시지 데이터",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않음 - 웹소켓으로 인증되지 않은 사용자",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "JWT 쿠키 인증")
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
    @Operation(summary = "채팅방 입장", description = "웹소켓을 통해 채팅방에 입장하고 참여자로 참여합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 입장 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않음 - 웹소켓으로 인증되지 않은 사용자",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "JWT 쿠키 인증")
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
     * 채팅방 연결 해제 (WebSocket 세션에서만 제거, 참여자 목록은 유지)
     * - 브라우저 탭 전환, 일시적 연결 끊김 등에 사용
     * - 채팅방 참여자 목록에서는 제거되지 않음
     */
    @MessageMapping("/room.disconnect")
    @Operation(summary = "채팅방 연결 해제", description = "채팅방 웹소켓 세션에서 연결을 해제합니다 (참여자 목록에서는 제거되지 않음)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 연결 해제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않음 - 웹소켓으로 인증되지 않은 사용자",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "JWT 쿠키 인증")
    public void disconnectFromRoom(@Payload @Valid RoomLeaveRequest request, SimpMessageHeaderAccessor headerAccessor) {
        // WebSocket 세션에서 인증된 사용자 ID 추출
        Long userId = getUserIdFromSession(headerAccessor);
        Long roomId = request.getRoomId();

        // 세션에서만 채팅방 정보 제거 (참여자 목록은 유지)
        removeCurrentRoomFromSession(headerAccessor);

        log.info("사용자 채팅방 연결 해제: userId={}, roomId={}", userId, roomId);
        
        // TODO: 다른 사용자들에게 임시 오프라인 상태 알림
    }

    /**
     * WebSocket 예외 처리
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    @Operation(summary = "웹소켓 예외 처리", description = "웹소켓 작업 중 발생하는 예외를 처리합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 큐로 오류 메시지 전송")
    })
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
