package com.example.chatapp.service.unit.chatroom;

import com.example.chatapp.domain.*;
import com.example.chatapp.domain.service.ChatRoomDomainService;
import com.example.chatapp.exception.ChatRoomException;
import com.example.chatapp.exception.UserException;
import com.example.chatapp.infrastructure.event.ChatEventPublisherService;
import com.example.chatapp.mapper.ChatRoomMapper;
import com.example.chatapp.repository.ChatRoomRepository;
import com.example.chatapp.repository.UserRepository;
import com.example.chatapp.service.impl.ChatRoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomInviteTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMapper chatRoomMapper;

    @Mock
    private ChatEventPublisherService chatEventPublisherService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomDomainService chatRoomDomainService;

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    private User admin;
    private User userToInvite;
    private User member;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        admin = User.create("admin", "encodedPassword");
        userToInvite = User.create("newuser", "encodedPassword");
        member = User.create("member", "encodedPassword");

        chatRoom = ChatRoom.create("Test Room", ChatRoomType.PRIVATE, admin);
    }

    @Test
    @DisplayName("관리자가 사용자를 초대할 수 있다")
    void adminCanInviteUser() {
        // given
        Long chatRoomId = 1L;
        Long userToInviteId = 2L;
        Long adminId = 3L;

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(userToInviteId)).thenReturn(Optional.of(userToInvite));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        // when
        chatRoomService.inviteUserToChatRoom(chatRoomId, userToInviteId, adminId);

        // then
        verify(chatRoomDomainService).inviteUser(chatRoom, userToInvite, admin);
        verify(chatEventPublisherService).publishUserJoinEvent(chatRoomId, userToInvite);
    }

    @Test
    @DisplayName("일반 멤버가 초대하면 예외가 발생한다")
    void memberCannotInvite() {
        // given
        Long chatRoomId = 1L;
        Long userToInviteId = 2L;
        Long memberId = 4L;

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(userToInviteId)).thenReturn(Optional.of(userToInvite));
        when(userRepository.findById(memberId)).thenReturn(Optional.of(member));

        doThrow(new IllegalArgumentException("채팅방 초대 권한이 없습니다"))
                .when(chatRoomDomainService).inviteUser(chatRoom, userToInvite, member);

        // when & then
        assertThatThrownBy(() ->
                chatRoomService.inviteUserToChatRoom(chatRoomId, userToInviteId, memberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채팅방 초대 권한이 없습니다");
    }

    @Test
    @DisplayName("채팅방이 존재하지 않으면 예외가 발생한다")
    void throwExceptionWhenChatRoomNotFound() {
        // given
        Long nonExistentRoomId = 999L;
        Long userToInviteId = 2L;
        Long inviterId = 3L;

        when(chatRoomRepository.findById(nonExistentRoomId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                chatRoomService.inviteUserToChatRoom(nonExistentRoomId, userToInviteId, inviterId))
                .isInstanceOf(ChatRoomException.class);
    }

    @Test
    @DisplayName("초대할 사용자가 존재하지 않으면 예외가 발생한다")
    void throwExceptionWhenUserNotFound() {
        // given
        Long chatRoomId = 1L;
        Long nonExistentUserId = 999L;
        Long inviterId = 3L;

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                chatRoomService.inviteUserToChatRoom(chatRoomId, nonExistentUserId, inviterId))
                .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("이미 참여한 사용자를 초대하면 예외가 발생한다")
    void throwExceptionWhenUserAlreadyJoined() {
        // given
        Long chatRoomId = 1L;
        Long existingMemberId = 4L;
        Long adminId = 3L;

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(existingMemberId)).thenReturn(Optional.of(member));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        doThrow(new IllegalArgumentException("이미 채팅방에 참여한 사용자입니다"))
                .when(chatRoomDomainService).inviteUser(chatRoom, member, admin);

        // when & then
        assertThatThrownBy(() ->
                chatRoomService.inviteUserToChatRoom(chatRoomId, existingMemberId, adminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 채팅방에 참여한 사용자입니다");
    }
}
