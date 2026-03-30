package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomJoinPasswordBody {
    /** Bắt buộc nếu phòng PRIVATE. */
    private String password;
}
