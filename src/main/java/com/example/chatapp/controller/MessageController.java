package com.example.chatapp.controller;

import com.example.chatapp.dto.request.MessageStatusUpdateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.dto.ErrorResponse;
import com.example.chatapp.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/messages")
@Slf4j
@Tag(name = "메시지", description = "메시지 관리 API")
public class MessageController {
    private final MessageService messageService;

    /**
     * 채팅방 메시지 조회 (페이지네이션)
     */
    @GetMapping("/room/{roomId}")
    @Operation(summary = "채팅방 메시지 조회", description = "특정 채팅방의 메시지를 페이지네이션으로 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않음",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "errorCode": "AUTH_ERROR",
                      "status": "UNAUTHORIZED",
                      "message": "인증이 필요합니다",
                      "timestamp": "2024-12-27T10:00:00"
                    }
                    """
                ))),
        @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "errorCode": "CHATROOM_NOT_FOUND",
                      "status": "NOT_FOUND",
                      "message": "채팅방을 찾을 수 없습니다",
                      "timestamp": "2024-12-27T10:00:00"
                    }
                    """
                )))
    })
    @SecurityRequirement(name = "JWT 쿠키 인증")
    public ResponseEntity<Page<MessageResponse>> findRoomMessages(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "페이지네이션 정보") @PageableDefault(size = 20) Pageable pageable) {
        log.debug("채팅방 메시지 조회: roomId={}, page={}, size={}",
                roomId, pageable.getPageNumber(), pageable.getPageSize());
        Page<MessageResponse> messages = messageService.findChatRoomMessages(roomId, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * 채팅방 최근 메시지 조회
     */
    @GetMapping("/room/{roomId}/recent")
    @Operation(summary = "최근 메시지 조회", description = "특정 채팅방의 최근 메시지를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "최근 메시지 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않음",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "errorCode": "AUTH_ERROR",
                      "status": "UNAUTHORIZED",
                      "message": "인증이 필요합니다",
                      "timestamp": "2024-12-27T10:00:00"
                    }
                    """
                ))),
        @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "errorCode": "CHATROOM_NOT_FOUND",
                      "status": "NOT_FOUND",
                      "message": "채팅방을 찾을 수 없습니다",
                      "timestamp": "2024-12-27T10:00:00"
                    }
                    """
                )))
    })
    @SecurityRequirement(name = "JWT 쿠키 인증")
    public ResponseEntity<List<MessageResponse>> findRecentRoomMessages(
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "조회할 최근 메시지 개수") @RequestParam(defaultValue = "50") int limit) {
        log.debug("최근 메시지 조회: roomId={}, limit={}", roomId, limit);
        List<MessageResponse> messages = messageService.findRecentChatRoomMessages(roomId, limit);
        return ResponseEntity.ok(messages);
    }

    /**
     * 메시지 상태 업데이트 (읽음 표시 등)
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "메시지 상태 업데이트", description = "메시지의 상태를 업데이트합니다 (예: 읽음 표시)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "메시지 상태 업데이트 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "errorCode": "VALIDATION_ERROR",
                      "status": "BAD_REQUEST",
                      "message": "요청 데이터가 유효하지 않습니다",
                      "timestamp": "2024-12-27T10:00:00",
                      "fieldErrors": [
                        {
                          "field": "status",
                          "rejectedValue": "INVALID",
                          "message": "유효한 상태값이 아닙니다"
                        }
                      ]
                    }
                    """
                ))),
        @ApiResponse(responseCode = "401", description = "인증 실패 - JWT 토큰이 없거나 유효하지 않음",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "errorCode": "AUTH_ERROR",
                      "status": "UNAUTHORIZED",
                      "message": "인증이 필요합니다",
                      "timestamp": "2024-12-27T10:00:00"
                    }
                    """
                ))),
        @ApiResponse(responseCode = "404", description = "메시지를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "errorCode": "MESSAGE_NOT_FOUND",
                      "status": "NOT_FOUND",
                      "message": "메시지를 찾을 수 없습니다",
                      "timestamp": "2024-12-27T10:00:00"
                    }
                    """
                )))
    })
    @SecurityRequirement(name = "JWT 쿠키 인증")
    public ResponseEntity<MessageResponse> updateStatus(
            @Parameter(description = "메시지 ID") @PathVariable Long id,
            @Valid @RequestBody MessageStatusUpdateRequest request) {
        log.debug("메시지 상태 업데이트: id={}, status={}, userId={}", id, request.getStatus(), request.getUserId());
        MessageResponse updated = messageService.updateMessageStatus(id, request.getUserId(), request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
