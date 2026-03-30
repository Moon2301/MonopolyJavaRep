package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MessageItemResponse {
    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String createdAt;
    private Boolean isRead;
}
