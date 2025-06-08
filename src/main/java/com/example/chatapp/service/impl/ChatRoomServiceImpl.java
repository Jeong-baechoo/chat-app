package com.example.chatapp.service.impl;

import com.example.chatapp.domain.*;
import com.example.chatapp.domain.service.ChatRoomDomainService;
import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.dto.response.ChatRoomSimpleResponse;
import com.example.chatapp.dto.response.UserResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.infrastructure.event.ChatEventPublisherService;
import com.example.chatapp.infrastructure.message.ChatEvent;
import com.example.chatapp.mapper.ChatRoomMapper;
import com.example.chatapp.mapper.UserMapper;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.EntityFinderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository participantRepo;
    private final ChatRoomMapper chatRoomMapper;
    private final UserMapper userMapper;
    private final ChatEventPublisherService chatEventPublisherService;
    private final EntityFinderService entityFinderService;
    private final ChatRoomDomainService chatRoomDomainService;

    @Override
    @Transactional
    public ChatRoomResponse createChatRoom(@Valid ChatRoomCreateRequest request) {
        // 먼저 사용자 존재 여부 확인
        User creator = findUserById(request.getCreatorId());

        // 도메인 서비스를 통한 채팅방 생성
        ChatRoom chatRoom = chatRoomDomainService.createChatRoom(
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

        // 이미 참여한 사용자인지 확인
        if (!participantRepo.existsByUserIdAndChatRoomId(userId, chatRoomId)) {
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
        validateUserIsRoomAdmin(userId, chatRoomId);

        chatRoomRepository.delete(chatRoom);
        log.debug("채팅방 삭제 완료: id={}", chatRoomId);

        // TODO: 채팅방 삭제 이벤트 발행 고려
    }

    @Override
    @Transactional
    public ChatRoomResponse removeParticipantFromChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = findChatRoomByIdOrThrow(chatRoomId);
        User user = findUserById(userId);

        // 참가자로 등록되어 있는지 확인
        if (!participantRepo.existsByUserIdAndChatRoomId(userId, chatRoomId)) {
            throw new ChatRoomException("해당 채팅방의 참가자가 아닙니다.");
        }

        // 채팅방에서 참가자 제거
        participantRepo.deleteByUserIdAndChatRoomId(userId, chatRoomId);
        ChatRoom updatedChatRoom = findChatRoomByIdOrThrow(chatRoomId);

        // 사용자 퇴장 이벤트 발행
        chatEventPublisherService.publishUserLeaveEvent(chatRoomId, user);

        log.info("사용자가 채팅방에서 퇴장했습니다: userId={}, chatRoomId={}", userId, chatRoomId);
        return chatRoomMapper.toResponse(updatedChatRoom);
    }

    // 내부 헬퍼 메소드


    private ChatRoom findChatRoomByIdOrThrow(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomException("채팅방을 찾을 수 없습니다"));
    }

    private User findUserById(Long userId) {
        return entityFinderService.findUserById(userId);
    }

    private void validateUserExists(Long userId) {
        entityFinderService.validateUserExists(userId);
    }

    private void validateUserIsRoomAdmin(Long userId, Long chatRoomId) {
        boolean isAdmin = participantRepo.findByUserIdAndChatRoomId(userId, chatRoomId)
                .map(participant -> participant.getRole() == ParticipantRole.ADMIN)
                .orElse(false);

        if (!isAdmin) {
            throw new ChatRoomException("채팅방을 삭제할 권한이 없습니다.");
        }
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
