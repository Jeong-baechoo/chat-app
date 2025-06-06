package com.example.chatapp.dto.request;

import com.example.chatapp.domain.ChatRoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateRequest {
    
    @NotBlank(message = "채팅방 이름은 필수입니다")
    @Size(min = 1, max = 100, message = "채팅방 이름은 1자 이상 100자 이하여야 합니다")
    private String name;
    
    @Size(max = 500, message = "채팅방 설명은 500자 이하여야 합니다")
    private String description;
    
    @NotNull(message = "채팅방 타입은 필수입니다")
    private ChatRoomType type;
    
    private Long creatorId; // 컨트롤러에서 설정
}
