package com.example.chatapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "chat_rooms")
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatRoomParticipant> participants = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 채팅방에 참여자 추가
     */
    public void addParticipant(ChatRoomParticipant participant) {
        if (participants == null) {
            participants = new HashSet<>();
        }
        participants.add(participant);
        participant.setChatRoom(this);
    }

    /**
     * 채팅방에서 참여자 제거
     */
    public void removeParticipant(ChatRoomParticipant participant) {
        participants.remove(participant);
        participant.setChatRoom(null);
    }
}
