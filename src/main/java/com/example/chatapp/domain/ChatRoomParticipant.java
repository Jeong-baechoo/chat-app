package com.example.chatapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    private ParticipantRole role;

    @Column(name = "notification_enabled")
    private Boolean notificationEnabled;

    private ChatRoomParticipant(User user, ChatRoom chatRoom, ParticipantRole role) {
        this.user = user;
        this.chatRoom = chatRoom;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
        this.notificationEnabled = true;
    }

    /**
     * 새로운 참여자 생성
     */
    public static ChatRoomParticipant of(User user, ChatRoom chatRoom, ParticipantRole role) {
        validateParameters(user, chatRoom, role);
        return new ChatRoomParticipant(user, chatRoom, role);
    }

    /**
     * 역할 변경
     */
    public void changeRole(ParticipantRole newRole, User requestor) {
        validateRoleChange(newRole, requestor);
        this.role = newRole;
    }

    /**
     * 알림 설정 토글
     */
    public void toggleNotification() {
        this.notificationEnabled = !this.notificationEnabled;
    }

    /**
     * 알림 설정 변경
     */
    public void setNotificationEnabled(boolean enabled) {
        this.notificationEnabled = enabled;
    }

    /**
     * ChatRoom 설정 (연관관계 편의 메서드) - 패키지 내부에서만 사용
     */
    void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    @PrePersist
    protected void onCreate() {
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
        if (this.notificationEnabled == null) {
            this.notificationEnabled = true;
        }
    }

    private static void validateParameters(User user, ChatRoom chatRoom, ParticipantRole role) {
        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다");
        }
        if (chatRoom == null) {
            throw new IllegalArgumentException("채팅방은 필수입니다");
        }
        if (role == null) {
            throw new IllegalArgumentException("참여자 역할은 필수입니다");
        }
    }

    private void validateRoleChange(ParticipantRole newRole, User requestor) {
        if (newRole == null) {
            throw new IllegalArgumentException("새로운 역할은 필수입니다");
        }
        if (this.role == newRole) {
            throw new IllegalArgumentException("이미 동일한 역할입니다");
        }
        // requestor가 null인 경우는 시스템에서 자동으로 변경하는 경우 (관리자 자동 승격 등)
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoomParticipant that = (ChatRoomParticipant) o;

        // 비즈니스 키 기반 비교: user + chatRoom 조합으로 유일성 보장
        return Objects.equals(user, that.user) &&
               Objects.equals(chatRoom, that.chatRoom);
    }

    @Override
    public int hashCode() {
        // 비즈니스 키 기반 해시코드
        return Objects.hash(user, chatRoom);
    }
}
