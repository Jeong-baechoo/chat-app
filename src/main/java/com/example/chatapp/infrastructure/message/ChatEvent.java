package com.example.chatapp.infrastructure.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatEvent {
    private String eventId;
    private ChatEventType eventType;
    private Long chatRoomId;
    private Long userId;
    private String username;
    private String content;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    
    // 메시지 관련 필드들
    private Long messageId;
    private String messageContent;
    private String messageStatus;
    
    public static ChatEvent messageEvent(Long messageId, String content, Long chatRoomId, 
                                       Long userId, String username) {
        return ChatEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(ChatEventType.MESSAGE_SENT)
                .messageId(messageId)
                .messageContent(content)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .username(username)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ChatEvent userJoinEvent(Long chatRoomId, Long userId, String username) {
        return ChatEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(ChatEventType.USER_JOINED)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .username(username)
                .content(username + "님이 채팅방에 입장했습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ChatEvent userLeaveEvent(Long chatRoomId, Long userId, String username) {
        return ChatEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(ChatEventType.USER_LEFT)
                .chatRoomId(chatRoomId)
                .userId(userId)
                .username(username)
                .content(username + "님이 채팅방에서 퇴장했습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ChatEvent roomCreatedEvent(Long chatRoomId, String roomName, Long creatorId, String creatorName) {
        return ChatEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType(ChatEventType.ROOM_CREATED)
                .chatRoomId(chatRoomId)
                .userId(creatorId)
                .username(creatorName)
                .content("새 채팅방 '" + roomName + "'이 생성되었습니다.")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
