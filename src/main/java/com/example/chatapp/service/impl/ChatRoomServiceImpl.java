package com.example.chatapp.service.impl;

import com.example.chatapp.domain.*;
import com.example.chatapp.dto.request.ChatRoomCreateRequest;
import com.example.chatapp.dto.response.ChatRoomResponse;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.UserException;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomServiceImpl implements ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRoomParticipantRepository participantRepo;
    private final ChatRoomMapper chatRoomMapper;

    @Override
    @Transactional
    public ChatRoomResponse createChatRoom(@Valid ChatRoomCreateRequest request) {
        ChatRoom chatRoom = chatRoomMapper.toEntity(request);
        User creator = findUserById(request.getCreatorId());
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        participantRepo.save(ChatRoomParticipant.builder()
                .user(creator)
                .chatRoom(savedChatRoom)
                .role(ParticipantRole.ADMIN)
                .notificationEnabled(true)
                .joinedAt(LocalDateTime.now())
                .build());

        log.debug("채팅방 생성: id={}, name={}, creator={}",
                savedChatRoom.getId(), savedChatRoom.getName(), creator.getUsername());

        return chatRoomMapper.toResponse(savedChatRoom);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> findAllChatRooms() {
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        log.debug("전체 채팅방 조회: {}개", rooms.size());
        return rooms.stream()
                .map(chatRoomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatRoomResponse> findChatRoomById(Long id) {
        return chatRoomRepository.findById(id).map(chatRoomMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> findChatRoomsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserException("사용자를 찾을 수 없습니다");
        }

        List<ChatRoomParticipant> participations = participantRepo.findByUserId(userId);
        log.debug("사용자 채팅방 조회: userId={}, 개수={}", userId, participations.size());

        return participations.stream()
                .map(p -> chatRoomMapper.toResponse(p.getChatRoom()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatRoomResponse addParticipantToChatRoom(Long chatRoomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomException("채팅방을 찾을 수 없습니다"));

        User user = findUserById(userId);

        if (!participantRepo.existsByUserIdAndChatRoomId(userId, chatRoomId)) {
            participantRepo.save(ChatRoomParticipant.builder()
                    .user(user)
                    .chatRoom(chatRoom)
                    .role(ParticipantRole.MEMBER)
                    .notificationEnabled(true)
                    .joinedAt(LocalDateTime.now())
                    .build());
            log.debug("채팅방 참여 추가: room={}, user={}", chatRoomId, userId);
        }

        return chatRoomMapper.toResponse(chatRoom);
    }

    @Override
    @Transactional
    public void deleteChatRoom(Long id) {
        if (!chatRoomRepository.existsById(id)) {
            throw new ChatRoomException("채팅방을 찾을 수 없습니다");
        }
        chatRoomRepository.deleteById(id);
        log.debug("채팅방 삭제: id={}", id);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException("사용자를 찾을 수 없습니다"));
    }
}
