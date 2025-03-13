package com.example.chatapp.mapper;

import com.example.chatapp.domain.ChatRoom;
import com.example.chatapp.domain.Message;
import com.example.chatapp.domain.MessageStatus;
import com.example.chatapp.domain.User;
import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final UserMapper userMapper;

    public MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .sender(userMapper.toResponse(message.getSender()))
                .chatRoomId(message.getChatRoom().getId())
                .status(message.getStatus())
                .timestamp(message.getTimestamp())
                .build();
    }

    public Message toEntity(MessageCreateRequest request, User sender, ChatRoom chatRoom) {
        return Message.builder()
                .content(request.getContent())
                .sender(sender)
                .chatRoom(chatRoom)
                .status(MessageStatus.SENT)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
