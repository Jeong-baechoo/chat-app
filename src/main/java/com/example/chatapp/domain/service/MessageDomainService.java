package com.example.chatapp.domain.service;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.Message;
import com.example.chatapp.domain.ParticipantRole;
import com.example.chatapp.domain.User;
import com.example.chatapp.exception.MessageException;
import org.springframework.stereotype.Service;

@Service
public class MessageDomainService {

    /**
     * 사용자가 메시지를 전송할 수 있는지 확인
     */
    public boolean canUserSendMessage(User user, ChatRoom chatRoom) {
        return chatRoom.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(user.getId()));
    }

    /**
     * 사용자가 메시지를 업데이트할 수 있는지 확인
     */
    public boolean canUserUpdateMessage(User user, Message message) {
        return message.getSender().getId().equals(user.getId());
    }

    /**
     * 사용자가 메시지를 삭제할 수 있는지 확인
     */
    public boolean canUserDeleteMessage(User user, Message message, ChatRoom chatRoom) {
        // 메시지 발신자이거나 채팅방 관리자인 경우 삭제 가능
        boolean isSender = message.getSender().getId().equals(user.getId());
        boolean isAdmin = chatRoom.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(user.getId())
                        && p.getRole() == ParticipantRole.ADMIN);

        return isSender || isAdmin;
    }

    /**
     * 메시지 삭제 권한 확인 및 예외 발생
     */
    public void validateDeletePermission(User user, Message message, ChatRoom chatRoom) {
        if (!canUserDeleteMessage(user, message, chatRoom)) {
            throw new MessageException("메시지를 삭제할 권한이 없습니다.");
        }
    }
}
