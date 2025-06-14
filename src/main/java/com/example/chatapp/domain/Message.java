package com.example.chatapp.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "messages",
        indexes = {
                @Index(name = "idx_chatroom_timestamp", columnList = "chat_room_id,timestamp"),
                @Index(name = "idx_sender_timestamp", columnList = "sender_id,timestamp"),
                @Index(name = "idx_timestamp", columnList = "timestamp"),
                @Index(name = "idx_status", columnList = "status")
        }
)
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // 메시지 발신자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // 메시지가 속한 채팅방
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    // 생성자
    private Message(String content, User sender, ChatRoom chatRoom) {
        validateContent(content);
        validateSender(sender);
        validateChatRoom(chatRoom);
        
        this.content = content;
        this.sender = sender;
        this.chatRoom = chatRoom;
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.SENT;
    }

    /**
     * 새로운 메시지 생성 (도메인 서비스에서 권한 검증 후 호출)
     */
    public static Message create(String content, User sender, ChatRoom chatRoom) {
        // 채팅방 참여자인지 검증
        if (!chatRoom.isParticipantById(sender.getId())) {
            throw new IllegalStateException("채팅방 참여자만 메시지를 보낼 수 있습니다");
        }
        return new Message(content, sender, chatRoom);
    }

    // 검증 메서드
    private static void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 필수입니다");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("메시지는 1000자를 초과할 수 없습니다");
        }
    }

    private static void validateSender(User sender) {
        if (sender == null) {
            throw new IllegalArgumentException("메시지 발신자는 필수입니다");
        }
    }

    private static void validateChatRoom(ChatRoom chatRoom) {
        if (chatRoom == null) {
            throw new IllegalArgumentException("채팅방은 필수입니다");
        }
    }

    /**
     * 메시지 상태 변경
     */
    public void updateStatus(MessageStatus newStatus) {
        validateStatusChange(newStatus);
        this.status = newStatus;
    }

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = MessageStatus.SENT;
        }
    }

    private void validateStatusChange(MessageStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("메시지 상태는 필수입니다");
        }
        
        // 상태 변경 규칙 검증
        if (this.status == MessageStatus.DELETED) {
            throw new IllegalStateException("삭제된 메시지의 상태는 변경할 수 없습니다");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        
        // ID가 있으면 ID 기반, 없으면 객체 동일성 비교
        if (id != null && message.id != null) {
            return Objects.equals(id, message.id);
        }
        return false; // 새로운 엔티티들은 같지 않음
    }

    @Override
    public int hashCode() {
        // 일관된 해시코드 (변경되지 않는 값 사용)
        return getClass().hashCode();
    }
}
