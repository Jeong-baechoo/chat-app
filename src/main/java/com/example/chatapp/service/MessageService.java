package com.example.chatapp.service;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.Message;
import com.example.chatapp.domain.MessageStatus;
import com.example.chatapp.domain.User;
import com.example.chatapp.dto.MessageDTO;
import com.example.chatapp.dto.MessageRequestDTO;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    private static final int MAX_MESSAGE_LENGTH = 1000;

    /**
     * 메시지 전송
     * @param messageRequest 메시지 요청 객체
     * @return 저장된 메시지의 DTO
     */
    @Transactional
    public MessageDTO sendMessage(MessageRequestDTO messageRequest) {
        // 유효성 검증
        if (!StringUtils.hasText(messageRequest.getContent())) {
            throw new MessageException("메시지 내용은 비어있을 수 없습니다.");
        }

        if (messageRequest.getContent().length() > MAX_MESSAGE_LENGTH) {
            throw new MessageException("메시지 길이는 " + MAX_MESSAGE_LENGTH + "자를 초과할 수 없습니다.");
        }

        // 사용자 조회
        User sender = userRepository.findById(messageRequest.getSenderId())
                .orElseThrow(() -> new UserException("발신자(ID: " + messageRequest.getSenderId() + ")를 찾을 수 없습니다."));

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(messageRequest.getChatRoomId())
                .orElseThrow(() -> new ChatRoomException("채팅방(ID: " + messageRequest.getChatRoomId() + ")을 찾을 수 없습니다."));

        boolean isParticipant = chatRoomParticipantRepository.existsByUserIdAndChatRoomId(sender.getId(), chatRoom.getId());
        if (!isParticipant) {
            throw new MessageException("사용자는 이 채팅방의 참여자가 아닙니다.");
        }

        // 메시지 생성
        Message message = Message.builder()
                .sender(sender)
                .chatRoom(chatRoom)
                .content(messageRequest.getContent())
                .status(MessageStatus.SENT)
                .timestamp(LocalDateTime.now())
                .build();

        // 저장 및 DTO 변환 후 반환
        Message savedMessage = messageRepository.save(message);
        return MessageDTO.fromEntity(savedMessage);
    }

    /**
     * 특정 채팅방의 메시지 목록 조회 (페이지네이션 적용)
     * @param chatRoomId 채팅방 ID
     * @param pageable 페이지네이션 정보
     * @return 메시지 DTO 페이지 객체
     */
    @Transactional(readOnly = true)
    public Page<MessageDTO> getChatRoomMessages(Long chatRoomId, Pageable pageable) {
        // 채팅방 존재 확인
        if (!chatRoomRepository.existsById(chatRoomId)) {
            throw new ChatRoomException("채팅방(ID: " + chatRoomId + ")을 찾을 수 없습니다.");
        }

        return messageRepository.findByChatRoomIdOrderByTimestampDesc(chatRoomId, pageable)
                .map(MessageDTO::fromEntity);
    }

    /**
     * 특정 채팅방의 최근 메시지 목록 조회 (제한된 개수)
     * @param chatRoomId 채팅방 ID
     * @param limit 조회할 메시지 개수
     * @return 메시지 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<MessageDTO> getRecentChatRoomMessages(Long chatRoomId, int limit) {
        // 채팅방 존재 확인
        if (!chatRoomRepository.existsById(chatRoomId)) {
            throw new ChatRoomException("채팅방(ID: " + chatRoomId + ")을 찾을 수 없습니다.");
        }

        return messageRepository.findTopByChatRoomIdOrderByTimestampDesc(chatRoomId, limit)
                .stream()
                .map(MessageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 메시지 ID로 조회
     * @param id 메시지 ID
     * @return 메시지 DTO (Optional)
     */
    @Transactional(readOnly = true)
    public MessageDTO findMessageById(Long id) {
        return messageRepository.findById(id)
                .map(MessageDTO::fromEntity)
                .orElseThrow(() -> new MessageException("메시지(ID: " + id + ")를 찾을 수 없습니다."));
    }

    /**
     * 메시지 상태 업데이트
     * @param messageId 메시지 ID
     * @param userId 요청한 사용자 ID (권한 확인용)
     * @param status 새로운 상태
     * @return 업데이트된 메시지 DTO
     */
    @Transactional
    public MessageDTO updateMessageStatus(Long messageId, Long userId, MessageStatus status) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("메시지(ID: " + messageId + ")를 찾을 수 없습니다."));

        // 메시지 발신자 또는 관리자만 상태 변경 가능
        if (!message.getSender().getId().equals(userId)) {
            throw new MessageException("메시지 상태를 변경할 권한이 없습니다.");
        }

        message.setStatus(status);
        return MessageDTO.fromEntity(messageRepository.save(message));
    }

    /**
     * 특정 사용자가 보낸 메시지 목록 조회
     * @param senderId 발신자 ID
     * @param pageable 페이지네이션 정보
     * @return 메시지 DTO 페이지 객체
     */
    @Transactional(readOnly = true)
    public Page<MessageDTO> getMessagesBySender(Long senderId, Pageable pageable) {
        // 사용자 존재 확인
        if (!userRepository.existsById(senderId)) {
            throw new UserException("사용자(ID: " + senderId + ")를 찾을 수 없습니다.");
        }

        return messageRepository.findBySenderIdOrderByTimestampDesc(senderId, pageable)
                .map(MessageDTO::fromEntity);
    }


    public void deleteMessage(Long id, Long userId) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new MessageException("메시지(ID: " + id + ")를 찾을 수 없습니다."));

        // 메시지 발신자 또는 관리자만 삭제 가능
        if (!message.getSender().getId().equals(userId)) {
            throw new MessageException("메시지를 삭제할 권한이 없습니다.");
        }

        messageRepository.delete(message);
    }
}
