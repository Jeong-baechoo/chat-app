package com.example.chatapp.dto.request;

import com.example.chatapp.domain.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "채팅방 생성 요청 정보를 담는 DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomCreateRequest {
    
    @Schema(description = "채팅방 이름 (1-100자)", example = "개발팀 회의방", required = true)
    @NotBlank(message = "채팅방 이름은 필수입니다")
    @Size(min = 1, max = 100, message = "채팅방 이름은 1자 이상 100자 이하여야 합니다")
    private String name;
    
    @Schema(description = "채팅방 설명 (최대 500자)", example = "매주 화요일 오후 2시 정기 회의", required = false)
    @Size(max = 500, message = "채팅방 설명은 500자 이하여야 합니다")
    private String description;
    
    @Schema(description = "채팅방 타입 (PUBLIC/PRIVATE)", example = "PUBLIC", required = true)
    @NotNull(message = "채팅방 타입은 필수입니다")
    private ChatRoomType type;
    
    @Schema(description = "채팅방 생성자 ID (서버에서 자동 설정)", hidden = true)
    private Long creatorId; // 컨트롤러에서 설정
}
