package com.example.chatapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
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

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.SENT;
    }
}
