package com.example.chatapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    /**
     * 메시지 상태 변경
     */
    public void updateStatus(MessageStatus newStatus) {
        validateStatusChange(newStatus);
        this.status = newStatus;
    }

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.SENT;
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
