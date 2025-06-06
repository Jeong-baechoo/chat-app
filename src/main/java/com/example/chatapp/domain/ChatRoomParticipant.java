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
@Table(name = "chat_room_participants",
        indexes = {
                @Index(name = "idx_user_chatroom", columnList = "user_id,chat_room_id")
        }
)
public class ChatRoomParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Enumerated(EnumType.STRING)
    private ParticipantRole role; // ADMIN, MEMBER 등

    @Column(name = "notification_enabled")
    private Boolean notificationEnabled;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
        this.notificationEnabled = true; // 기본값 설정
    }
}
