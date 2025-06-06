package com.example.chatapp.service;

import com.example.chatapp.dto.request.MessageCreateRequest;
import com.example.chatapp.exception.MessageException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 메시지 검증 컴포넌트
 * 메시지 관련 요청의 유효성 검증을 담당합니다.
 */
@Component
public class MessageValidator {
    private static final int MAX_MESSAGE_LENGTH = 1000;

    /**
     * 메시지 생성 요청 검증
     * 
     * @param request 메시지 생성 요청
     * @throws MessageException 요청이 유효하지 않은 경우
     */
    public void validateMessageRequest(MessageCreateRequest request) {
        if (request.getChatRoomId() == null) {
            throw new MessageException("채팅방 ID는 필수입니다");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new MessageException("메시지 내용은 필수입니다");
        }
        if (request.getContent().length() > MAX_MESSAGE_LENGTH) {
            throw new MessageException("메시지 길이는 최대 " + MAX_MESSAGE_LENGTH + "자를 초과할 수 없습니다");
        }
    }
}
