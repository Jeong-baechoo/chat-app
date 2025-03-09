package com.example.chatapp.service;

import com.example.chatapp.domain.*;
import com.example.chatapp.dto.ChatRoomCreateDTO;
import com.example.chatapp.dto.ChatRoomDTO;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.repository.ChatRoomParticipantRepository;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Transactional
    public ChatRoomDTO createChatRoom(ChatRoomCreateDTO dto) {
        // 채팅방 생성
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(dto.getName());
        chatRoom.setType(dto.getType());
        chatRoom.setParticipants(new ArrayList<>()); // 참가자 리스트 초기화

        // 생성자 조회
        Long creatorId = dto.getCreatorId();
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserException("사용자를 찾을 수 없습니다."));

        // 채팅방 먼저 저장 (ID 생성)
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 생성자를 ADMIN 역할로 참가자로 추가
        ChatRoomParticipant participant = ChatRoomParticipant.builder()
                .user(creator)
                .chatRoom(savedChatRoom)
                .role(ParticipantRole.ADMIN) // 생성자는 관리자 역할 부여
                .notificationEnabled(true)
                .joinedAt(LocalDateTime.now())
                .build();

        // 참가자 정보 저장
        chatRoomParticipantRepository.save(participant);

        // 저장된 채팅방 DTO 반환
        return ChatRoomDTO.fromEntity(savedChatRoom);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDTO> findAllChatRooms() {
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        System.out.println("Found " + rooms.size() + " chat rooms");
        // 엔티티 리스트를 DTO 리스트로 변환
        return rooms.stream()
                .map(ChatRoomDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ChatRoomDTO> findChatRoomById(Long id) {
        return chatRoomRepository.findById(id)
                .map(ChatRoomDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDTO> findChatRoomsByUser(Long userId) {
        List<ChatRoomParticipant> participations = chatRoomParticipantRepository.findByUserId(userId);
        return participations.stream()
                .map(participant -> ChatRoomDTO.fromEntity(participant.getChatRoom()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatRoomDTO addParticipantToChatRoom(Long chatRoomId, Long userId) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("채팅방을 찾을 수 없습니다."));

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 이미 참여 중인지 확인
        boolean isAlreadyParticipant = chatRoomParticipantRepository
                .existsByUserIdAndChatRoomId(userId, chatRoomId);

        // 참여자가 아닌 경우에만 추가
        if (!isAlreadyParticipant) {
            ChatRoomParticipant participant = ChatRoomParticipant.builder()
                    .user(user)
                    .chatRoom(chatRoom)
                    .role(ParticipantRole.MEMBER) // 일반 멤버로 추가
                    .notificationEnabled(true)
                    .joinedAt(LocalDateTime.now())
                    .build();

            chatRoomParticipantRepository.save(participant);
        }

        // 업데이트된 채팅방 반환
        return ChatRoomDTO.fromEntity(chatRoom);
    }

    @Transactional
    public void deleteChatRoom(Long id) {
        if (!chatRoomRepository.existsById(id)) {
            throw new RuntimeException("채팅방을 찾을 수 없습니다.");
        }
        chatRoomRepository.deleteById(id);
    }
}
