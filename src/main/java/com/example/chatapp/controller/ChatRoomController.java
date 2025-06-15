package com.example.chatapp.controller;

import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.dto.response.ChatRoomSimpleResponse;
import com.example.chatapp.dto.ErrorResponse;
import com.example.chatapp.infrastructure.auth.AuthContext;
import com.example.chatapp.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
@Slf4j
@Tag(name = "채팅방", description = "채팅방 관리 API")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;
    private final AuthContext authContext;

    /**
     * 전체 채팅방 목록 조회
     */
    @GetMapping
    @Operation(summary = "전체 채팅방 목록 조회", description = "사용 가능한 모든 채팅방 목록을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공")
    })
    public ResponseEntity<List<ChatRoomSimpleResponse>> getAllRooms() {
        log.debug("전체 채팅방 조회 API 요청");
        List<ChatRoomSimpleResponse> response = chatRoomService.findAllChatRoomsSimple();
        return ResponseEntity.ok(response);
    }

    /**
     * 현재 사용자가 참여한 채팅방 목록 조회
     */
    @GetMapping("/me")
    @Operation(summary = "내 채팅방 목록 조회", description = "현재 사용자가 참여한 채팅방 목록을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 채팅방 목록 조회 성공"),
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
                )))
    })
    @SecurityRequirement(name = "JWT 쿠키 인증")
    public ResponseEntity<List<ChatRoomResponse>> getMyRooms() {
        Long userId = authContext.getCurrentUserId();
        log.debug("사용자별 채팅방 조회 API 요청: userId={}", userId);
        List<ChatRoomResponse> response = chatRoomService.findChatRoomsByUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 채팅방 상세 정보 조회
     */
    @GetMapping("/{id}")
    @Operation(summary = "채팅방 상세 정보 조회", description = "특정 채팅방의 상세 정보를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 상세 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(
                        name = "채팅방 없음",
                        value = """
                        {
                          "errorCode": "CHATROOM_NOT_FOUND",
                          "status": "NOT_FOUND",
                          "message": "채팅방을 찾을 수 없습니다",
                          "timestamp": "2024-12-27T10:00:00",
                          "fieldErrors": null
                        }
                        """
                    )
                ))
    })
    public ResponseEntity<ChatRoomResponse> getRoomById(@PathVariable Long id) {
        log.debug("채팅방 상세 조회 API 요청: id={}", id);
        return chatRoomService.findChatRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 채팅방 생성
     */
    @PostMapping
    @Operation(summary = "채팅방 생성", description = "인증된 사용자를 관리자로 하는 새 채팅방을 생성합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
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
                          "field": "name",
                          "rejectedValue": "",
                          "message": "채팅방 이름은 필수입니다"
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
                )))
    })
    @SecurityRequirement(name = "JWT 쿠키 인증")
    public ResponseEntity<ChatRoomResponse> createRoom(@Valid @RequestBody ChatRoomCreateRequest request) {
        Long userId = authContext.getCurrentUserId();
        log.debug("채팅방 생성 API 요청: name={}, type={}, userId={}",
                request.getName(), request.getType(), userId);

        // 인증된 사용자 ID를 creatorId로 설정
        request.setCreatorId(userId);

        ChatRoomResponse response = chatRoomService.createChatRoom(request);

        // 생성된 리소스의 URI를 헤더에 포함
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * 채팅방 참여
     */
    @PostMapping("/{id}/join")
    @Operation(summary = "채팅방 참여", description = "기존 채팅방에 참여자로 참여합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 참여 성공"),
        @ApiResponse(responseCode = "400", description = "이미 참여 중이거나 잘못된 요청",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "errorCode": "CHATROOM_ALREADY_JOINED",
                      "status": "BAD_REQUEST",
                      "message": "이미 채팅방에 참여 중입니다",
                      "timestamp": "2024-12-27T10:00:00"
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
    public ResponseEntity<ChatRoomResponse> joinRoom(@PathVariable Long id) {
        Long userId = authContext.getCurrentUserId();
        log.debug("채팅방 참여 API 요청: roomId={}, userId={}", id, userId);
        ChatRoomResponse response = chatRoomService.addParticipantToChatRoom(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 채팅방 탈퇴 (참여자 목록에서 완전히 제거)
     */
    @PostMapping("/{id}/leave")
    @Operation(summary = "채팅방 나가기", description = "채팅방을 영구적으로 나갑니다 (참여자 목록에서 제거)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "채팅방 나가기 성공"),
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
        @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없거나 참여자가 아님",
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
    public ResponseEntity<Void> leaveChatRoom(@PathVariable Long id) {
        Long userId = authContext.getCurrentUserId();
        log.debug("채팅방 탈퇴 API 요청: roomId={}, userId={}", id, userId);
        chatRoomService.removeParticipantFromChatRoom(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 채팅방 삭제 (관리자 권한 필요)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "채팅방 삭제", description = "채팅방을 삭제합니다 (관리자 권한 필요)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "채팅방 삭제 성공"),
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
        @ApiResponse(responseCode = "403", description = "권한 없음 - 사용자가 해당 채팅방의 관리자가 아님",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "errorCode": "CHATROOM_FORBIDDEN",
                      "status": "FORBIDDEN",
                      "message": "채팅방 관리자 권한이 필요합니다",
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
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        log.debug("채팅방 삭제 API 요청: id={}", id);
        Long currentUserId = authContext.getCurrentUserId();
        chatRoomService.deleteChatRoom(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
