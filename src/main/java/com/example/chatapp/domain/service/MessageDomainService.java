package com.example.chatapp.domain.service;

import com.example.chatapp.domain.*;
import com.example.chatapp.domain.exception.MessageDomainException;
import org.springframework.stereotype.Component;

/**
 * 메시지 도메인 서비스
 * 메시지와 관련된 순수한 도메인 규칙을 처리합니다.
 */
@Component
public class MessageDomainService {


    /**
     * 사용자가 메시지를 업데이트할 수 있는지 확인
     * 메시지 발신자만 메시지를 업데이트할 수 있습니다.
     *
     * @param user 사용자
     * @param message 메시지
     * @return 메시지 업데이트 가능 여부
     */
    public boolean canUserUpdateMessage(User user, Message message) {
        return message.getSender().getId().equals(user.getId());
    }

    /**t
     * 사용자가 메시지를 삭제할 수 있는지 확인
     * 메시지 발신자이거나 채팅방 관리자만 메시지를 삭제할 수 있습니다.
     *
     * @param user 사용자
     * @param message 메시지
     * @param chatRoom 채팅방
     * @return 메시지 삭제 가능 여부
     */
    public boolean canUserDeleteMessage(User user, Message message, ChatRoom chatRoom) {
        // 메시지 발신자인 경우
        boolean isSender = message.getSender().getId().equals(user.getId());

        // 채팅방 관리자인 경우
        boolean isAdmin = chatRoom.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(user.getId())
                        && p.getRole() == ParticipantRole.ADMIN);

        return isSender || isAdmin;
    }

    /**
     * 메시지 삭제 권한 확인 및 예외 발생
     *
     * @param user 사용자
     * @param message 메시지
     * @param chatRoom 채팅방
     * @throws MessageException 삭제 권한이 없는 경우
     */
    public void validateDeletePermission(User user, Message message, ChatRoom chatRoom) {
        if (!canUserDeleteMessage(user, message, chatRoom)) {
            throw new MessageDomainException("메시지를 삭제할 권한이 없습니다");
        }
    }

}
