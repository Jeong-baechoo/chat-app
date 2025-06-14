package com.example.chatapp.domain.service;

import com.example.chatapp.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 채팅방 도메인 서비스
 * 채팅방과 참여자 관리에 대한 복합적인 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class ChatRoomDomainService {


    /**
     * 채팅방에 사용자 초대 (관리자만 가능)
     */
    public void inviteUser(ChatRoom chatRoom, User userToInvite, User inviter) {
        validateInvitePermission(chatRoom, inviter);
        validateCanJoin(chatRoom, userToInvite);
        validateParticipantLimit(chatRoom);
        validateDuplicateUser(chatRoom, userToInvite);

        chatRoom.addParticipantInternal(userToInvite, ParticipantRole.MEMBER);
    }

    /**
     * 채팅방에 자유 참여 (권한 검증 없이)
     */
    public void joinChatRoom(ChatRoom chatRoom, User user) {
        validateCanJoin(chatRoom, user);
        validateParticipantLimit(chatRoom);
        validateDuplicateUser(chatRoom, user);

        chatRoom.addParticipantInternal(user, ParticipantRole.MEMBER);
    }

    /**
     * 채팅방에서 사용자 제거
     */
    public void removeUser(ChatRoom chatRoom, User userToRemove, User requestor) {
        validateRemovePermission(chatRoom, userToRemove, requestor);
        validateUserIsParticipant(chatRoom, userToRemove);

        chatRoom.removeParticipantInternal(userToRemove);
    }

    /**
     * 사용자가 채팅방에서 나가기
     */
    public void leaveRoom(ChatRoom chatRoom, User user) {
        validateUserIsParticipant(chatRoom, user);
        chatRoom.removeParticipantInternal(user);
    }

    /**
     * 채팅방명 변경
     */
    public void changeChatRoomName(ChatRoom chatRoom, String newName, User requestor) {
        validateAdminPermission(chatRoom, requestor);
        chatRoom.changeName(newName);
    }

    /**
     * 참여자 역할 변경
     */
    public void changeParticipantRole(ChatRoom chatRoom, User targetUser, ParticipantRole newRole, User requestor) {
        validateAdminPermission(chatRoom, requestor);
        validateUserIsParticipant(chatRoom, targetUser);
        validateRoleChange(newRole, requestor, targetUser);

        chatRoom.changeParticipantRoleInternal(targetUser, newRole);
    }

    // 검증 메서드들
    private void validateInvitePermission(ChatRoom chatRoom, User inviter) {
        Objects.requireNonNull(inviter, "초대자는 필수입니다");
        if (!chatRoom.isAdmin(inviter)) {
            throw new IllegalArgumentException("채팅방 초대 권한이 없습니다");
        }
    }

    private void validateCanJoin(ChatRoom chatRoom, User user) {
        Objects.requireNonNull(user, "사용자는 필수입니다");
        Objects.requireNonNull(chatRoom, "채팅방은 필수입니다");
    }

    private void validateParticipantLimit(ChatRoom chatRoom) {
        if (chatRoom.isFull()) {
            throw new IllegalStateException("채팅방 참여자 수가 한계에 도달했습니다");
        }
    }

    private void validateDuplicateUser(ChatRoom chatRoom, User user) {
        if (chatRoom.isParticipant(user)) {
            throw new IllegalArgumentException("이미 채팅방에 참여한 사용자입니다");
        }
    }

    private void validateRemovePermission(ChatRoom chatRoom, User targetUser, User requestor) {
        Objects.requireNonNull(targetUser, "제거할 사용자는 필수입니다");
        Objects.requireNonNull(requestor, "요청자는 필수입니다");

        // 자신을 제거하거나 관리자가 다른 사용자를 제거하는 경우만 허용
        if (!targetUser.equals(requestor) && !chatRoom.isAdmin(requestor)) {
            throw new IllegalArgumentException("참여자를 제거할 권한이 없습니다");
        }
    }

    private void validateUserIsParticipant(ChatRoom chatRoom, User user) {
        if (!chatRoom.isParticipant(user)) {
            throw new IllegalArgumentException("채팅방에 참여하지 않은 사용자입니다");
        }
    }

    private void validateAdminPermission(ChatRoom chatRoom, User requestor) {
        Objects.requireNonNull(requestor, "요청자는 필수입니다");
        if (!chatRoom.isAdmin(requestor)) {
            throw new IllegalArgumentException("관리자 권한이 필요합니다");
        }
    }

    private void validateRoleChange(ParticipantRole newRole, User requestor, User targetUser) {
        Objects.requireNonNull(newRole, "새로운 역할은 필수입니다");

        // 자기 자신의 역할은 변경할 수 없음 (관리자 권한 남용 방지)
        if (requestor.equals(targetUser)) {
            throw new IllegalArgumentException("자신의 역할은 변경할 수 없습니다");
        }
    }
}
