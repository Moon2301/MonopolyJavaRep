package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomInviteRequest {
    private Long roomId;
    /** Mời theo profile id (bạn đã kết bạn). */
    private Long toUserProfileId;
    /** Mời trực tiếp theo username (vẫn phải đã kết bạn). */
    private String username;
}
