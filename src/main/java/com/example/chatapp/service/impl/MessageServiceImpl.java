// src/main/java/com/example/chatapp/service/impl/MessageServiceImpl.java
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
        // 유효성 검증
        validateMessageRequest(request);

        // 사용자 조회
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new UserException("발신자(ID: " + request.getSenderId() + ")를 찾을 수 없습니다."));

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new ChatRoomException("채팅방(ID: " + request.getChatRoomId() + ")을 찾을 수 없습니다."));

        // 참여자 검증
        validateChatRoomParticipant(sender.getId(), chatRoom.getId());

        // 메시지 생성
        Message message = messageMapper.toEntity(request, sender, chatRoom);

        // 저장 및 DTO 변환 후 반환
        Message savedMessage = messageRepository.save(message);

        // 이벤트 발행
        publishMessageCreatedEvent(savedMessage);

        log.debug("메시지 저장 완료: id={}, senderId={}, chatRoomId={}",
                savedMessage.getId(), sender.getId(), chatRoom.getId());

        return messageMapper.toResponse(savedMessage);
    }

    /**
     * 특정 채팅방의 메시지 목록 조회 (페이지네이션 적용)
     * @param chatRoomId 채팅방 ID
     * @param pageable 페이지네이션 정보
     * @return 메시지 응답 DTO 페이지 객체
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getChatRoomMessages(Long chatRoomId, Pageable pageable) {
        // 채팅방 존재 확인
        validateChatRoomExists(chatRoomId);

        return messageRepository.findByChatRoomIdOrderByTimestampDesc(chatRoomId, pageable)
                .map(messageMapper::toResponse);
    }

    /**
     * 특정 채팅방의 최근 메시지 목록 조회 (제한된 개수)
     * @param chatRoomId 채팅방 ID
     * @param limit 조회할 메시지 개수
     * @return 메시지 응답 DTO 리스트
     */
    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getRecentChatRoomMessages(Long chatRoomId, int limit) {
        // 채팅방 존재 확인
        validateChatRoomExists(chatRoomId);

        return messageRepository.findTopByChatRoomIdOrderByTimestampDesc(chatRoomId, limit)
                .stream()
                .map(messageMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 메시지 ID로 조회
     * @param id 메시지 ID
     * @return 메시지 응답 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public MessageResponse findMessageById(Long id) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new MessageException("메시지(ID: " + id + ")를 찾을 수 없습니다."));
        return messageMapper.toResponse(message);
    }

    /**
     * 메시지 상태 업데이트
     * @param messageId 메시지 ID
     * @param userId 요청한 사용자 ID (권한 확인용)
     * @param status 새로운 상태
     * @return 업데이트된 메시지 응답 DTO
     */
    @Override
    @Transactional
    public MessageResponse updateMessageStatus(Long messageId, Long userId, MessageStatus status) {
        Message message = findMessageEntityById(messageId);
        User user = findUserEntityById(userId);

        // 권한 검증
        if (!messageDomainService.canUserUpdateMessage(user, message)) {
            throw new MessageException("메시지 상태를 변경할 권한이 없습니다.");
        }

        message.setStatus(status);
        Message updatedMessage = messageRepository.save(message);

        log.debug("메시지 상태 업데이트: id={}, status={}, userId={}",
                messageId, status, userId);

        return messageMapper.toResponse(updatedMessage);
    }

    /**
     * 특정 사용자가 보낸 메시지 목록 조회
     * @param senderId 발신자 ID
     * @param pageable 페이지네이션 정보
     * @return 메시지 응답 DTO 페이지 객체
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesBySender(Long senderId, Pageable pageable) {
        // 사용자 존재 확인
        validateUserExists(senderId);

        return messageRepository.findBySenderIdOrderByTimestampDesc(senderId, pageable)
                .map(messageMapper::toResponse);
    }

    /**
     * 메시지 삭제
     * @param id 메시지 ID
     * @param userId 삭제 요청 사용자 ID
     */
    @Override
    @Transactional
    public void deleteMessage(Long id, Long userId) {
        Message message = findMessageEntityById(id);
        User user = findUserEntityById(userId);
        ChatRoom chatRoom = message.getChatRoom();

        // 도메인 서비스를 통해 삭제 권한 검증
        messageDomainService.validateDeletePermission(user, message, chatRoom);

        messageRepository.delete(message);
        log.debug("메시지 삭제 완료: id={}, userId={}", id, userId);
    }

    // 메시지 요청 유효성 검증
    private void validateMessageRequest(MessageCreateRequest request) {
        if (request.getSenderId() == null) {
            throw new MessageException("발신자 ID는 null일 수 없습니다.");
        }

        if (request.getChatRoomId() == null) {
            throw new MessageException("채팅방 ID는 null일 수 없습니다.");
        }

        if (!StringUtils.hasText(request.getContent())) {
            throw new MessageException("메시지 내용은 비어있을 수 없습니다.");
        }

        if (request.getContent().length() > MAX_MESSAGE_LENGTH) {
            throw new MessageException("메시지 길이는 " + MAX_MESSAGE_LENGTH + "자를 초과할 수 없습니다.");
        }
    }

    // 채팅방 참여자 검증
    private void validateChatRoomParticipant(Long userId, Long chatRoomId) {
        boolean isParticipant = chatRoomParticipantRepository.existsByUserIdAndChatRoomId(userId, chatRoomId);
        if (!isParticipant) {
            throw new MessageException("사용자는 이 채팅방의 참여자가 아닙니다.");
        }
    }

    // 채팅방 존재 확인
    private void validateChatRoomExists(Long chatRoomId) {
        if (!chatRoomRepository.existsById(chatRoomId)) {
            throw new ChatRoomException("채팅방(ID: " + chatRoomId + ")을 찾을 수 없습니다.");
        }
    }

    // 사용자 존재 확인
    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserException("사용자(ID: " + userId + ")를 찾을 수 없습니다.");
        }
    }

    // 메시지 엔티티 조회
    private Message findMessageEntityById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageException("메시지(ID: " + messageId + ")를 찾을 수 없습니다."));
    }

    // 사용자 엔티티 조회
    private User findUserEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException("사용자(ID: " + userId + ")를 찾을 수 없습니다."));
    }

    // 메시지 생성 이벤트 발행
    private void publishMessageCreatedEvent(Message message) {
        MessageCreatedEvent event = new MessageCreatedEvent(
                message.getId(),
                message.getSender().getId(),
                message.getChatRoom().getId(),
                message.getTimestamp()
        );

        eventPublisher.publishEvent(event);
        log.debug("메시지 생성 이벤트 발행: messageId={}, chatRoomId={}",
                message.getId(), message.getChatRoom().getId());
    }
}
