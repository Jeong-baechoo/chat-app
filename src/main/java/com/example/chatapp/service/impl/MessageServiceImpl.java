package com.example.chatapp.service.impl;

import com.example.chatapp.domain.*;
import com.example.chatapp.domain.service.MessageDomainService;
import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.event.MessageCreatedEvent;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.mapper.MessageMapper;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final MessageDomainService messageDomainService;
    private final MessageMapper messageMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAX_MESSAGE_LENGTH = 1000;

    /**
     * 메시지 전송
     * @param request 메시지 생성 요청 객체
     * @return 저장된 메시지의 응답 DTO
     */
    @Override
    @Transactional
    public MessageResponse sendMessage(MessageCreateRequest request) {
        validateMessageRequest(request);

        User sender = findUserById(request.getSenderId());
        ChatRoom chatRoom = findChatRoomById(request.getChatRoomId());

        validateChatRoomParticipant(sender.getId(), chatRoom.getId());

        Message message = messageMapper.toEntity(request, sender, chatRoom);
        Message savedMessage = messageRepository.save(message);

        publishMessageCreatedEvent(savedMessage);

        log.debug("메시지 저장: id={}, senderId={}, chatRoomId={}",
                savedMessage.getId(), sender.getId(), chatRoom.getId());

        return messageMapper.toResponse(savedMessage);
    }

    // 채팅방 메시지 조회 (페이징)
    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getChatRoomMessages(Long chatRoomId, Pageable pageable) {
        validateChatRoomExists(chatRoomId);
        return messageRepository.findByChatRoomIdOrderByTimestampDesc(chatRoomId, pageable)
                .map(messageMapper::toResponse);
    }

    // 최근 메시지 조회
    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getRecentChatRoomMessages(Long chatRoomId, int limit) {
        validateChatRoomExists(chatRoomId);
        return messageRepository.findTopByChatRoomIdOrderByTimestampDesc(chatRoomId, limit)
                .stream()
                .map(messageMapper::toResponse)
                .collect(Collectors.toList());
    }

    // 메시지 ID로 조회
    @Override
    @Transactional(readOnly = true)
    public MessageResponse findMessageById(Long id) {
        return messageMapper.toResponse(findMessageEntityById(id));
    }

    // 메시지 상태 업데이트
    @Override
    @Transactional
    public MessageResponse updateMessageStatus(Long messageId, Long userId, MessageStatus status) {
        Message message = findMessageEntityById(messageId);
        User user = findUserById(userId);

        if (!messageDomainService.canUserUpdateMessage(user, message)) {
            throw new MessageException("메시지 상태 변경 권한 없음");
        }

        message.setStatus(status);
        Message updatedMessage = messageRepository.save(message);

        log.debug("메시지 상태 업데이트: id={}, status={}", messageId, status);

        return messageMapper.toResponse(updatedMessage);
    }

    // 발신자별 메시지 조회
    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesBySender(Long senderId, Pageable pageable) {
        validateUserExists(senderId);
        return messageRepository.findBySenderIdOrderByTimestampDesc(senderId, pageable)
                .map(messageMapper::toResponse);
    }

    // 메시지 삭제
    @Override
    @Transactional
    public void deleteMessage(Long id, Long userId) {
        Message message = findMessageEntityById(id);
        User user = findUserById(userId);

        messageDomainService.validateDeletePermission(user, message, message.getChatRoom());

        messageRepository.delete(message);
        log.debug("메시지 삭제: id={}", id);
    }

    // === 검증 메서드 ===

    private void validateMessageRequest(MessageCreateRequest request) {
        if (request.getSenderId() == null) {
            throw new MessageException("발신자 ID 누락");
        }
        if (request.getChatRoomId() == null) {
            throw new MessageException("채팅방 ID 누락");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new MessageException("메시지 내용 누락");
        }
        if (request.getContent().length() > MAX_MESSAGE_LENGTH) {
            throw new MessageException("메시지 길이 초과: 최대 " + MAX_MESSAGE_LENGTH + "자");
        }
    }

    // === 엔티티 조회 메서드 ===

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException("사용자 없음: " + userId));
    }

    private ChatRoom findChatRoomById(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomException("채팅방 없음: " + chatRoomId));
    }

    private Message findMessageEntityById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("메시지 없음: " + messageId));
    }

    // === 유효성 검사 메서드 ===

    private void   validateChatRoomParticipant(Long userId, Long chatRoomId) {
        boolean isParticipant = chatRoomParticipantRepository.existsByUserIdAndChatRoomId(userId, chatRoomId);
        if (!isParticipant) {
            throw new MessageException("채팅방 참여자 아님");
        }
    }

    private void validateChatRoomExists(Long chatRoomId) {
        if (!chatRoomRepository.existsById(chatRoomId)) {
            throw new ChatRoomException("채팅방 없음: " + chatRoomId);
        }
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserException("사용자 없음: " + userId);
        }
    }

    // === 이벤트 발행 ===

    private void publishMessageCreatedEvent(Message message) {
        MessageCreatedEvent event = new MessageCreatedEvent(
                message.getId(),
                message.getSender().getId(),
                message.getChatRoom().getId(),
                message.getTimestamp()
        );
        eventPublisher.publishEvent(event);
    }
}
