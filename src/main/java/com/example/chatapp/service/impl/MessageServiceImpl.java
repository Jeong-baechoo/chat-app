package com.example.chatapp.service.impl;

import com.example.chatapp.domain.*;
import com.example.chatapp.domain.service.MessageDomainService;
import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.mapper.MessageMapper;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.service.EntityFinderService;
import com.example.chatapp.service.MessageEventPublisher;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.MessageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 메시지 서비스 구현 클래스
 * 메시지 생성, 조회, 수정, 삭제 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final MessageDomainService messageDomainService;
    private final MessageMapper messageMapper;
    private final EntityFinderService entityFinder;
    private final MessageValidator validator;
    private final MessageEventPublisher eventPublisher;

    /**
     * 메시지 전송
     * 메시지 생성 요청을 검증하고, 새 메시지를 저장한 후 이벤트를 발행합니다.
     *
     * @param request 메시지 생성 요청 객체
     * @return 저장된 메시지의 응답 DTO
     * @throws MessageException 메시지 요청이 유효하지 않은 경우
     */
    @Override
    @Transactional
    public MessageResponse sendMessage(MessageCreateRequest request) {
        // 메시지 요청 검증
        validator.validateMessageRequest(request);

        // 엔티티 조회
        User sender = entityFinder.findUserById(request.getSenderId());
        ChatRoom chatRoom = entityFinder.findChatRoomById(request.getChatRoomId());

        // 채팅방 참여자 여부 검증
        if (!messageDomainService.canUserSendMessage(sender, chatRoom)) {
            throw new MessageException("채팅방 참여자만 메시지를 보낼 수 있습니다");
        }

        // 메시지 생성 및 저장
        Message message = messageDomainService.createMessage(request.getContent(), sender, chatRoom);
        Message savedMessage = messageRepository.save(message);

        // 이벤트 발행
        eventPublisher.publishMessageCreatedEvent(savedMessage);

        log.debug("메시지 저장 완료: id={}, senderId={}, chatRoomId={}",
                savedMessage.getId(), sender.getId(), chatRoom.getId());

        return messageMapper.toResponse(savedMessage);
    }

    /**
     * 채팅방 메시지 조회 (페이징)
     *
     * @param chatRoomId 채팅방 ID
     * @param pageable 페이징 정보
     * @return 페이징된 메시지 응답 목록
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getChatRoomMessages(Long chatRoomId, Pageable pageable) {
        entityFinder.validateChatRoomExists(chatRoomId);
        return messageRepository.findByChatRoomIdOrderByTimestampDesc(chatRoomId, pageable)
                .map(messageMapper::toResponse);
    }

    /**
     * 채팅방의 최근 메시지 조회
     *
     * @param chatRoomId 채팅방 ID
     * @param limit 조회할 메시지 수
     * @return 최근 메시지 응답 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getRecentChatRoomMessages(Long chatRoomId, int limit) {
        entityFinder.validateChatRoomExists(chatRoomId);
        return messageRepository.findTopByChatRoomIdOrderByTimestampDesc(chatRoomId, limit)
                .stream()
                .map(messageMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 메시지 ID로 조회
     *
     * @param id 메시지 ID
     * @return 메시지 응답 객체
     * @throws MessageException 메시지가 존재하지 않는 경우
     */
    @Override
    @Transactional(readOnly = true)
    public MessageResponse findMessageById(Long id) {
        Message message = entityFinder.findMessageById(id);
        return messageMapper.toResponse(message);
    }

    /**
     * 메시지 상태 업데이트
     *
     * @param messageId 메시지 ID
     * @param userId 상태를 업데이트하려는 사용자 ID
     * @param status 변경할 상태
     * @return 업데이트된 메시지 응답
     * @throws MessageException 메시지가 존재하지 않거나 권한이 없는 경우
     */
    @Override
    @Transactional
    public MessageResponse updateMessageStatus(Long messageId, Long userId, MessageStatus status) {
        Message message = entityFinder.findMessageById(messageId);
        User user = entityFinder.findUserById(userId);

        if (!messageDomainService.canUserUpdateMessage(user, message)) {
            throw new MessageException("메시지 상태를 변경할 권한이 없습니다");
        }

        Message updatedMessage = messageDomainService.updateMessageStatus(message, status);
        Message savedMessage = messageRepository.save(updatedMessage);

        log.debug("메시지 상태 업데이트 완료: id={}, status={}", messageId, status);

        return messageMapper.toResponse(savedMessage);
    }

    /**
     * 발신자별 메시지 조회
     *
     * @param senderId 발신자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 메시지 응답 목록
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesBySender(Long senderId, Pageable pageable) {
        entityFinder.validateUserExists(senderId);
        return messageRepository.findBySenderIdOrderByTimestampDesc(senderId, pageable)
                .map(messageMapper::toResponse);
    }

    /**
     * 메시지 삭제
     *
     * @param id 메시지 ID
     * @param userId 삭제를 요청하는 사용자 ID
     * @throws MessageException 메시지가 존재하지 않거나 권한이 없는 경우
     */
    @Override
    @Transactional
    public void deleteMessage(Long id, Long userId) {
        Message message = entityFinder.findMessageById(id);
        User user = entityFinder.findUserById(userId);

        messageDomainService.validateDeletePermission(user, message, message.getChatRoom());

        messageRepository.delete(message);
        log.debug("메시지 삭제 완료: id={}", id);
    }
}
