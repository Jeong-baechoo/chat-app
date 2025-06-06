package com.example.chatapp.service;

import com.example.chatapp.constants.ErrorMessages;
import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.Message;
import com.example.chatapp.domain.User;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 엔티티 조회 서비스
 * 각 엔티티 조회 로직을 중앙화하여 코드 중복을 방지합니다.
 */
@Service
@RequiredArgsConstructor
public class EntityFinderService {
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;

    /**
     * ID로 사용자 엔티티 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 엔티티
     * @throws UserException 사용자가 존재하지 않는 경우
     */
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorMessages.USER_NOT_FOUND + ": " + userId));
    }

    /**
     * ID로 채팅방 엔티티 조회
     *
     * @param chatRoomId 채팅방 ID
     * @return 채팅방 엔티티
     * @throws ChatRoomException 채팅방이 존재하지 않는 경우
     */
    public ChatRoom findChatRoomById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomException(ErrorMessages.CHATROOM_NOT_FOUND + ": " + chatRoomId));
    }

    /**
     * ID로 메시지 엔티티 조회
     *
     * @param messageId 메시지 ID
     * @return 메시지 엔티티
     * @throws MessageException 메시지가 존재하지 않는 경우
     */
    public Message findMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageException(ErrorMessages.MESSAGE_NOT_FOUND + ": " + messageId));
    }

    /**
     * 채팅방 존재 여부 검증
     *
     * @param chatRoomId 채팅방 ID
     * @throws ChatRoomException 채팅방이 존재하지 않는 경우
     */
    public void validateChatRoomExists(Long chatRoomId) {
         if (!chatRoomRepository.existsById(chatRoomId)) {
            throw new ChatRoomException(ErrorMessages.CHATROOM_NOT_FOUND + ": " + chatRoomId);
        }
    }

    /**
     * 사용자 존재 여부 검증
     *
     * @param userId 사용자 ID
     * @throws UserException 사용자가 존재하지 않는 경우
     */
    public void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserException(ErrorMessages.USER_NOT_FOUND + ": " + userId);
        }
    }
}
