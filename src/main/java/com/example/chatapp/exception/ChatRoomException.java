package com.example.chatapp.exception;

/**
 * 채팅방 관련 비즈니스 예외
 */
public class ChatRoomException extends BaseException {
    
    public ChatRoomException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public ChatRoomException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public ChatRoomException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
    
    // 구체적인 예외 생성을 위한 정적 팩토리 메서드
    public static ChatRoomException notFound(Long roomId) {
        return new ChatRoomException(ErrorCode.CHATROOM_NOT_FOUND, 
            String.format("ID %d에 해당하는 채팅방을 찾을 수 없습니다", roomId));
    }
    
    public static ChatRoomException accessDenied() {
        return new ChatRoomException(ErrorCode.CHATROOM_ACCESS_DENIED);
    }
    
    public static ChatRoomException adminRequired() {
        return new ChatRoomException(ErrorCode.CHATROOM_ADMIN_REQUIRED);
    }
    
    public static ChatRoomException alreadyJoined() {
        return new ChatRoomException(ErrorCode.CHATROOM_ALREADY_JOINED);
    }
    
    public static ChatRoomException notParticipant() {
        return new ChatRoomException(ErrorCode.CHATROOM_NOT_PARTICIPANT);
    }
}
