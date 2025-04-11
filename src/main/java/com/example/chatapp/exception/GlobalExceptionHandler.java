package com.example.chatapp.exception;

import com.example.chatapp.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserException.class)
  public ResponseEntity<ErrorResponse> handleUserException(UserException e) {

    // 사용자를 찾을 수 없는 경우는 404 NOT FOUND로 처리
    if (e.getMessage().contains("찾을 수 없습니다")) {
      ErrorResponse error = new ErrorResponse("USER_NOT_FOUND", e.getMessage(), LocalDateTime.now());
      return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // 이미 존재하는 사용자명의 경우 409 CONFLICT로 처리
    if (e.getMessage().contains("이미 사용 중인 사용자명") ||
            e.getMessage().contains("이미 존재하는")) {
      ErrorResponse error = new ErrorResponse("USER_CONFLICT", e.getMessage(), LocalDateTime.now());
      return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // 그 외 사용자 관련 예외는 400 BAD REQUEST로 처리
    ErrorResponse error = new ErrorResponse("USER_ERROR", e.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ChatRoomException.class)
  public ResponseEntity<ErrorResponse> handleChatRoomException(ChatRoomException e) {
    // 채팅방을 찾을 수 없는 경우는 404 NOT FOUND로 처리
    if (e.getMessage().contains("찾을 수 없습니다")) {
      ErrorResponse error = new ErrorResponse("CHATROOM_NOT_FOUND", e.getMessage(), LocalDateTime.now());
      return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // 권한 없음 에러는 403 FORBIDDEN으로 처리
    if (e.getMessage().contains("권한이 없습니다")) {
      ErrorResponse error = new ErrorResponse("CHATROOM_FORBIDDEN", e.getMessage(), LocalDateTime.now());
      return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // 그 외 채팅방 관련 예외는 400 BAD REQUEST로 처리
    ErrorResponse error = new ErrorResponse("CHATROOM_ERROR", e.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MessageException.class)
  public ResponseEntity<ErrorResponse> handleMessageException(MessageException e) {
    // 메시지를 찾을 수 없는 경우는 404 NOT FOUND로 처리
    if (e.getMessage().contains("찾을 수 없습니다")) {
      ErrorResponse error = new ErrorResponse("MESSAGE_NOT_FOUND", e.getMessage(), LocalDateTime.now());
      return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // 권한 없음 에러는 403 FORBIDDEN으로 처리
    if (e.getMessage().contains("권한이 없습니다")) {
      ErrorResponse error = new ErrorResponse("MESSAGE_FORBIDDEN", e.getMessage(), LocalDateTime.now());
      return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // 그 외 메시지 관련 예외는 400 BAD REQUEST로 처리
    ErrorResponse error = new ErrorResponse("MESSAGE_ERROR", e.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException e) {
    ErrorResponse error = new ErrorResponse("AUTH_ERROR", e.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
    // 이미 존재하는 사용자명 등의 충돌 오류
    if (e.getMessage().contains("이미 사용 중인") || e.getMessage().contains("이미 존재하는")) {
      ErrorResponse error = new ErrorResponse("CONFLICT_ERROR", e.getMessage(), LocalDateTime.now());
      return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    // 그 외 입력값 관련 오류
    ErrorResponse error = new ErrorResponse("INVALID_INPUT", e.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
    ErrorResponse error = new ErrorResponse("SERVER_ERROR", e.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
