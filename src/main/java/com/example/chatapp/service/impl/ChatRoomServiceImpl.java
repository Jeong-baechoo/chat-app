package com.example.chatapp.service.impl;

import com.example.chatapp.domain.*;
import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.dto.response.ChatRoomSimpleResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.infrastructure.event.ChatEventPublisherService;
import com.example.chatapp.mapper.ChatRoomMapper;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.ChatRoomService;
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
    private final UserRepository userRepository;
    private final ChatRoomParticipantRepository participantRepo;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatEventPublisherService eventPublisher;

    @Override
    @Transactional
    public ChatRoomResponse createChatRoom(@Valid ChatRoomCreateRequest request) {
        // 먼저 사용자 존재 여부 확인
        User creator = findUserById(request.getCreatorId());

        // 채팅방 생성
        ChatRoom chatRoom = chatRoomMapper.toEntity(request);
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 참여자 추가 - 생성자는 ADMIN 권한으로 추가
        addParticipantToChatRoomInternal(savedChatRoom, creator, ParticipantRole.ADMIN);

        // 이벤트 발행 로직을 별도 서비스로 위임
        eventPublisher.publishRoomCreatedEvent(savedChatRoom, creator);

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
            // 참여자 추가 - 일반 사용자는 MEMBER 권한으로 추가
            addParticipantToChatRoomInternal(chatRoom, user, ParticipantRole.MEMBER);

            // 이벤트 발행 로직을 별도 서비스로 위임
            eventPublisher.publishUserJoinEvent(chatRoomId, user);
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

    // 내부 헬퍼 메소드

    private void addParticipantToChatRoomInternal(ChatRoom chatRoom, User user, ParticipantRole role) {
        ChatRoomParticipant participant = ChatRoomParticipant.builder()
                .user(user)
                .chatRoom(chatRoom)
                .role(role)
                .notificationEnabled(true)
                .joinedAt(LocalDateTime.now())
                .build();

        participantRepo.save(participant);
    }

    private ChatRoom findChatRoomByIdOrThrow(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomException("채팅방을 찾을 수 없습니다"));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException("사용자를 찾을 수 없습니다"));
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserException("사용자를 찾을 수 없습니다");
        }
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
