package com.example.chatapp.service.impl;

import com.example.chatapp.domain.*;
import com.example.chatapp.domain.service.MessageDomainService;
import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import com.example.chatapp.exception.MessageException;
import com.example.chatapp.infrastructure.event.ChatEventPublisherService;
import com.example.chatapp.mapper.MessageMapper;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.MessageRepository;
import com.example.chatapp.service.EntityFinderService;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.MessageValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 메시지 서비스 구현 클래스
 * 메시지 생성, 조회, 수정, 삭제 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // 기본적으로 모든 메소드는 읽기 전용 트랜잭션 사용
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final MessageDomainService messageDomainService;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final MessageMapper messageMapper;
    private final EntityFinderService entityFinder;
    private final MessageValidator validator;
    private final ChatEventPublisherService eventPublisher;

    /**
     * 메시지 전송
     * 메시지 생성 요청을 검증하고, 새 메시지를 저장한 후 이벤트를 발행합니다.
     *
     * @param request 메시지 생성 요청 DTO
     * @return 메시지 응답 DTO
     * @throws MessageException 메시지 요청이 유효하지 않은 경우
     */
    @Override
    @Transactional
    public MessageResponse sendMessage(MessageCreateRequest request, Long senderId) {
        // 메시지 요청 검증
        validator.validateMessageRequest(request);

        // 메시지 전송 권한 확인 (채팅방 참여자인지)
        ChatRoomParticipant participant = validateAndGetParticipant(senderId, request.getChatRoomId());
        User sender = participant.getUser();
        ChatRoom chatRoom = participant.getChatRoom();

        // 도메인 서비스를 통해 메시지 생성
        Message message = messageDomainService.createMessage(request.getContent(), sender, chatRoom);
        Message savedMessage = messageRepository.save(message);

        // 이벤트 발행 로직을 별도 서비스로 위임
        eventPublisher.publishMessageEvent(savedMessage, sender);

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
    public Page<MessageResponse> findChatRoomMessages(Long chatRoomId, Pageable pageable) {
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
    public List<MessageResponse> findRecentChatRoomMessages(Long chatRoomId, int limit) {
        entityFinder.validateChatRoomExists(chatRoomId);
        // 최적화된 쿼리 사용 (FETCH JOIN으로 N+1 문제 해결)
        List<Message> messages = messageRepository.findTopByChatRoomIdWithSenderAndRoomOrderByTimestampDesc(chatRoomId, limit);

        // 적은 양의 데이터를 처리할 때는 스트림 대신 반복문 사용
        List<MessageResponse> responses = new ArrayList<>(messages.size());
        for (Message message : messages) {
            responses.add(messageMapper.toResponse(message));
        }
        return responses;
    }

    /**
     * 메시지 ID로 조회
     *
     * @param id 메시지 ID
     * @return 메시지 응답 객체
     * @throws MessageException 메시지가 존재하지 않는 경우
     */
    @Override
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

        // 권한 확인
        if (!messageDomainService.canUserUpdateMessage(user, message)) {
            throw new MessageException("메시지 상태를 변경할 권한이 없습니다");
        }

        // 도메인 서비스를 통한 상태 업데이트
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
    public Page<MessageResponse> findMessagesBySender(Long senderId, Pageable pageable) {
        entityFinder.validateUserExists(senderId);
        // 최적화된 쿼리 사용 (FETCH JOIN으로 N+1 문제 해결)
        return messageRepository.findBySenderIdWithSenderAndRoomOrderByTimestampDesc(senderId, pageable)
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

    // 내부 헬퍼 메소드

    /**
     * 사용자가 채팅방의 참여자인지 확인하고 참여자 정보 반환
     * 최적화된 쿼리 사용 (FETCH JOIN으로 N+1 문제 해결)
     */
    private ChatRoomParticipant validateAndGetParticipant(Long userId, Long chatRoomId) {
        return chatRoomParticipantRepository
                .findByUserIdAndChatRoomIdWithUserAndChatRoom(userId, chatRoomId)
                .orElseThrow(() -> new MessageException("채팅방 참여자만 메시지를 보낼 수 있습니다"));
    }
}
