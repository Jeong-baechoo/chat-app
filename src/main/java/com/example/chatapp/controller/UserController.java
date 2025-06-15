package com.example.chatapp.controller;

import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.dto.ErrorResponse;
import com.example.chatapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
@Tag(name = "사용자", description = "사용자 관리 API")
public class UserController {
    private final UserService userService;

    @GetMapping
    @Operation(summary = "전체 사용자 조회", description = "등록된 모든 사용자 목록을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 목록 조회 성공"),
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
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.findAllUsers());
    }

    @GetMapping("/isLoggedIn")
    @Operation(summary = "로그인한 사용자 조회", description = "현재 로그인한 사용자 목록을 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인한 사용자 목록 조회 성공"),
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
    public ResponseEntity<List<UserResponse>> getLoggedInUsers() {
        return ResponseEntity.ok(userService.findLoggedInUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "ID로 사용자 조회", description = "사용자 ID로 사용자 정보를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
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
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "errorCode": "USER_NOT_FOUND",
                      "status": "NOT_FOUND",
                      "message": "사용자를 찾을 수 없습니다",
                      "timestamp": "2024-12-27T10:00:00"
                    }
                    """
                )))
    })
    @SecurityRequirement(name = "JWT 쿠키 인증")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "사용자 ID") @PathVariable Long id) {
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "사용자 이름으로 사용자 조회", description = "사용자 이름으로 사용자 정보를 조회합니다")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
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
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "errorCode": "USER_NOT_FOUND",
                      "status": "NOT_FOUND",
                      "message": "사용자를 찾을 수 없습니다",
                      "timestamp": "2024-12-27T10:00:00"
                    }
                    """
                )))
    })
    @SecurityRequirement(name = "JWT 쿠키 인증")
    public ResponseEntity<UserResponse> getUserByUsername(
            @Parameter(description = "사용자 이름") @PathVariable String username) {
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
