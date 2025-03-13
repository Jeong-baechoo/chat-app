// src/main/java/com/example/chatapp/service/impl/ChatRoomServiceImpl.java
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
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final ChatRoomMapper chatRoomMapper;

    @Override
    @Transactional
    public ChatRoomResponse createChatRoom(@Valid ChatRoomCreateRequest request) {
        // 채팅방 엔티티 생성
        ChatRoom chatRoom = chatRoomMapper.toEntity(request);

        // 생성자 조회
        User creator = userRepository.findById(request.getCreatorId())
                .orElseThrow(() -> new UserException("사용자를 찾을 수 없습니다."));

        // 채팅방 저장 (ID 생성)
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 생성자를 ADMIN 역할로 참가자로 추가
        ChatRoomParticipant participant = ChatRoomParticipant.builder()
                .user(creator)
                .chatRoom(savedChatRoom)
                .role(ParticipantRole.ADMIN)
                .notificationEnabled(true)
                .joinedAt(LocalDateTime.now())
                .build();

        // 참가자 정보 저장
        chatRoomParticipantRepository.save(participant);

        log.debug("채팅방 생성 완료: id={}, name={}, creator={}",
                savedChatRoom.getId(), savedChatRoom.getName(), creator.getUsername());

        // 응답 DTO 반환
        return chatRoomMapper.toResponse(savedChatRoom);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> findAllChatRooms() {
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        log.debug("전체 채팅방 조회: {}개 조회됨", rooms.size());

        return rooms.stream()
                .map(chatRoomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatRoomResponse> findChatRoomById(Long id) {
        return chatRoomRepository.findById(id)
                .map(chatRoomMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> findChatRoomsByUser(Long userId) {
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new UserException("사용자를 찾을 수 없습니다.");
        }

        List<ChatRoomParticipant> participations = chatRoomParticipantRepository.findByUserId(userId);
        log.debug("사용자 채팅방 조회: userId={}, {}개 조회됨", userId, participations.size());

        return participations.stream()
                .map(participant -> chatRoomMapper.toResponse(participant.getChatRoom()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatRoomResponse addParticipantToChatRoom(Long chatRoomId, Long userId) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomException("채팅방을 찾을 수 없습니다."));

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("사용자를 찾을 수 없습니다."));

        // 이미 참여 중인지 확인
        boolean isAlreadyParticipant = chatRoomParticipantRepository
                .existsByUserIdAndChatRoomId(userId, chatRoomId);

        // 참여자가 아닌 경우에만 추가
        if (!isAlreadyParticipant) {
            ChatRoomParticipant participant = ChatRoomParticipant.builder()
                    .user(user)
                    .chatRoom(chatRoom)
                    .role(ParticipantRole.MEMBER)
                    .notificationEnabled(true)
                    .joinedAt(LocalDateTime.now())
                    .build();

            chatRoomParticipantRepository.save(participant);
            log.debug("채팅방 참여 추가: chatRoomId={}, userId={}", chatRoomId, userId);
        } else {
            log.debug("이미 채팅방에 참여 중: chatRoomId={}, userId={}", chatRoomId, userId);
        }

        // 업데이트된 채팅방 반환
        return chatRoomMapper.toResponse(chatRoom);
    }

    @Override
    @Transactional
    public void deleteChatRoom(Long id) {
        if (!chatRoomRepository.existsById(id)) {
            throw new ChatRoomException("채팅방을 찾을 수 없습니다.");
        }

        chatRoomRepository.deleteById(id);
        log.debug("채팅방 삭제 완료: id={}", id);
    }
}
