package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FriendListItemResponse {
    private Long friendId;
    private Long userProfileId;
    private String username;
    private String avatarUrl;
    private String status;
    private Long unreadMessages;
    /** Lời mời đến — người chơi có thể bấm chấp nhận. */
    private Boolean canAccept;
    /** Lời mời đi — đang chờ đối phương. */
    private Boolean pendingOutgoing;
    /** ONLINE | PLAYING | OFFLINE — chỉ gần đúng (heartbeat API). */
    private String presenceStatus;
}
