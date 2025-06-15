package com.example.chatapp.service.impl;

import com.example.chatapp.domain.*;
import com.example.chatapp.domain.service.ChatRoomDomainService;
import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.dto.response.ChatRoomSimpleResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.infrastructure.event.ChatEventPublisherService;
import com.example.chatapp.mapper.ChatRoomMapper;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.EntityFinderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatEventPublisherService chatEventPublisherService;
    private final EntityFinderService entityFinderService;
    private final ChatRoomDomainService chatRoomDomainService;

    @Override
    @Transactional
    public ChatRoomResponse createChatRoom(ChatRoomCreateRequest request) {
        // 먼저 사용자 존재 여부 확인
        User creator = findUserById(request.getCreatorId());

        // 채팅방 생성 (도메인 엔티티의 정적 팩토리 메서드 사용)
        ChatRoom chatRoom = ChatRoom.create(
                request.getName(),
                request.getType(),
                creator
        );

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 이벤트 발행 로직을 별도 서비스로 위임
        chatEventPublisherService.publishRoomCreatedEvent(savedChatRoom, creator);

        log.debug("채팅방 생성 완료: roomId={}, creatorId={}",
                savedChatRoom.getId(), creator.getId());

        return chatRoomMapper.toResponse(savedChatRoom);
    }

    @Override
    public List<ChatRoomResponse> findAllChatRooms() {
        List<ChatRoom> rooms = chatRoomRepository.findAllWithParticipants();
        return mapChatRoomsToResponses(rooms);
    }

    @Override
    public List<ChatRoomSimpleResponse> findAllChatRoomsSimple() {
        return chatRoomRepository.findAllRoomsAsSimpleDto();
    }

    @Override
    public Optional<ChatRoomResponse> findChatRoomById(Long id) {
        return chatRoomRepository.findByIdWithParticipants(id)
                .map(chatRoomMapper::toResponse);
    }

    @Override
    public List<ChatRoomResponse> findChatRoomsByUser(Long userId) {
        validateUserExists(userId);
        List<ChatRoom> rooms = chatRoomRepository.findAllByParticipantUserId(userId);
        return mapChatRoomsToResponses(rooms);
    }

    @Override
    @Transactional
    public ChatRoomResponse addParticipantToChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = findChatRoomByIdOrThrow(chatRoomId);
        User user = findUserById(userId);

        // 이미 참여한 사용자인지 도메인에서 확인
        if (!chatRoom.isParticipantById(userId)) {
            // 도메인 서비스를 통한 자유 참여
            chatRoomDomainService.joinChatRoom(chatRoom, user);

            // 이벤트 발행 로직을 별도 서비스로 위임
            chatEventPublisherService.publishUserJoinEvent(chatRoomId, user);
        }

        return chatRoomMapper.toResponse(chatRoom);
    }

    @Override
    @Transactional
    public void deleteChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = findChatRoomByIdOrThrow(chatRoomId);

        // 도메인에서 권한 검증
        chatRoom.validateCanDelete(userId);

        chatRoomRepository.delete(chatRoom);
        log.debug("채팅방 삭제 완료: id={}", chatRoomId);

        // TODO: 채팅방 삭제 이벤트 발행 고려
    }

    @Override
    @Transactional
    public ChatRoomResponse removeParticipantFromChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = findChatRoomByIdOrThrow(chatRoomId);
        User user = findUserById(userId);

        // 참가자로 등록되어 있는지 도메인에서 확인
        if (!chatRoom.isParticipantById(userId)) {
            throw ChatRoomException.notParticipant();
        }

        // 도메인 서비스를 통한 참가자 제거
        chatRoomDomainService.leaveRoom(chatRoom, user);

        // 사용자 퇴장 이벤트 발행
        chatEventPublisherService.publishUserLeaveEvent(chatRoomId, user);

        log.info("사용자가 채팅방에서 퇴장했습니다: userId={}, chatRoomId={}", userId, chatRoomId);
        return chatRoomMapper.toResponse(chatRoom);
    }

    // 내부 헬퍼 메소드


    private ChatRoom findChatRoomByIdOrThrow(Long chatRoomId) {
        return entityFinderService.findChatRoomById(chatRoomId);
    }

    private User findUserById(Long userId) {
        return entityFinderService.findUserById(userId);
    }

    private void validateUserExists(Long userId) {
        entityFinderService.validateUserExists(userId);
    }


    private List<ChatRoomResponse> mapChatRoomsToResponses(List<ChatRoom> rooms) {
        // 적은 양의 데이터를 처리할 때는 스트림 대신 반복문 사용
        List<ChatRoomResponse> responses = new ArrayList<>(rooms.size());
        for (ChatRoom room : rooms) {
            responses.add(chatRoomMapper.toResponse(room));
        }
        return responses;
    }
}
