package com.example.chatapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_rooms")
public class ChatRoom {
    private static final int MAX_PARTICIPANTS = 100;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "name", nullable = false, length = 100))
    private ChatRoomName name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatRoomParticipant> participants = new HashSet<>();

    private ChatRoom(ChatRoomName name, ChatRoomType type) {
        this.name = name;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.participants = new HashSet<>();
    }

    /**
     * 새로운 채팅방 생성
     */
    public static ChatRoom create(String name, ChatRoomType type, User creator) {
        ChatRoomName chatRoomName = ChatRoomName.of(name);
        ChatRoom chatRoom = new ChatRoom(chatRoomName, type);
        chatRoom.addParticipant(creator, ParticipantRole.ADMIN);
        return chatRoom;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * 채팅방명 변경 (도메인 서비스에서만 호출)
     */
    public void changeName(String newName) {
        this.name = ChatRoomName.of(newName);
    }

    /**
     * 초기 생성 시 첫 번째 참여자 추가 (생성자에서만 사용)
     */
    private void addParticipant(User user, ParticipantRole role) {
        ChatRoomParticipant participant = ChatRoomParticipant.of(user, this, role);
        this.participants.add(participant);
    }

    /**
     * 참여자 추가 (도메인 서비스에서만 호출)
     */
    public void addParticipantInternal(User user, ParticipantRole role) {
        ChatRoomParticipant participant = ChatRoomParticipant.of(user, this, role);
        this.participants.add(participant);
    }

    /**
     * 참여자 제거 (도메인 서비스에서만 호출)
     */
    public void removeParticipantInternal(User user) {
        ChatRoomParticipant participant = findParticipant(user);
        
        if (participant.getRole() == ParticipantRole.ADMIN) {
            handleAdminRemoval();
        }
        
        this.participants.remove(participant);
    }

    /**
     * 참여자 역할 변경 (도메인 서비스에서만 호출)
     */
    public void changeParticipantRoleInternal(User targetUser, ParticipantRole newRole) {
        ChatRoomParticipant participant = findParticipant(targetUser);
        participant.changeRole(newRole, null); // 도메인 서비스에서 검증 후 호출
    }

    // 조회 메서드들
    public boolean isAdmin(User user) {
        return participants.stream()
                .anyMatch(p -> p.getUser().equals(user) && p.getRole() == ParticipantRole.ADMIN);
    }

    public int getParticipantCount() {
        return participants.size();
    }

    public boolean isFull() {
        return participants.size() >= MAX_PARTICIPANTS;
    }

    public List<User> getAdmins() {
        return participants.stream()
                .filter(p -> p.getRole() == ParticipantRole.ADMIN)
                .map(ChatRoomParticipant::getUser)
                .toList();
    }

    public boolean isParticipant(User user) {
        return participants.stream()
                .anyMatch(p -> p.getUser().equals(user));
    }

    // 내부 유틸리티 메서드들 (package-private)

    private ChatRoomParticipant findParticipant(User user) {
        return participants.stream()
                .filter(p -> p.getUser().equals(user))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("채팅방에 참여하지 않은 사용자입니다"));
    }

    private void handleAdminRemoval() {
        List<User> admins = getAdmins();
        if (admins.size() == 1) {
            // 마지막 관리자가 나갈 때 처리
            List<ChatRoomParticipant> members = participants.stream()
                    .filter(p -> p.getRole() == ParticipantRole.MEMBER)
                    .toList();
            
            if (!members.isEmpty()) {
                // 가장 먼저 참여한 멤버를 관리자로 승격
                ChatRoomParticipant newAdmin = members.stream()
                        .min((p1, p2) -> p1.getJoinedAt().compareTo(p2.getJoinedAt()))
                        .orElseThrow();
                newAdmin.changeRole(ParticipantRole.ADMIN, null); // 시스템에서 자동 승격
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoom chatRoom = (ChatRoom) o;
        
        // ID가 있으면 ID 기반, 없으면 객체 동일성 비교
        if (id != null && chatRoom.id != null) {
            return Objects.equals(id, chatRoom.id);
        }
        return false; // 새로운 엔티티들은 같지 않음
    }

    @Override
    public int hashCode() {
        // 일관된 해시코드 (변경되지 않는 값 사용)
        return getClass().hashCode();
    }
}
