package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class NotificationItemResponse {
    private Long notificationId;
    private String type;
    private String title;
    private String body;
    private Boolean read;
    private String createdAt;
    private Long roomId;
    private String roomCode;
    private Long senderUserProfileId;
    private String senderUsername;
}
