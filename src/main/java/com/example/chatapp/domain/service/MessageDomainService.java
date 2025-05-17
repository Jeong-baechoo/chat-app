package com.example.chatapp.domain.service;

import com.example.chatapp.domain.*;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 메시지 도메인 서비스
 * 메시지와 관련된 핵심 비즈니스 규칙을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class MessageDomainService {

    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    /**
     * 사용자가 메시지를 전송할 수 있는지 확인
     *
     * @param user 사용자
     * @param chatRoom 채팅방
     * @return 메시지 전송 가능 여부
     */
    public boolean canUserSendMessage(User user, ChatRoom chatRoom) {
        return chatRoomParticipantRepository.existsByUserAndChatRoom(user, chatRoom);
    }

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
            throw new MessageException("메시지를 삭제할 권한이 없습니다");
        }
    }

    /**
     * 새 메시지 생성
     *
     * @param content 메시지 내용
     * @param sender 발신자
     * @param chatRoom 채팅방
     * @return 생성된 메시지 엔티티
     */
    public Message createMessage(String content, User sender, ChatRoom chatRoom) {
        return Message.builder()
                .content(content)
                .sender(sender)
                .chatRoom(chatRoom)
                .status(MessageStatus.SENT)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 메시지 상태 업데이트
     *
     * @param message 메시지
     * @param status 업데이트할 상태
     * @return 업데이트된 메시지
     */
    public Message updateMessageStatus(Message message, MessageStatus status) {
        message.setStatus(status);
        return message;
    }
}
