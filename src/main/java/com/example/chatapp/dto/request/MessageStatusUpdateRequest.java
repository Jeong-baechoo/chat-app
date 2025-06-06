// src/main/java/com/example/chatapp/dto/request/MessageStatusUpdateRequest.java
package com.example.chatapp.dto.request;

import com.example.chatapp.domain.MessageStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusUpdateRequest {
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotNull(message = "메시지 상태는 필수입니다.")
    private MessageStatus status;
}
