package com.game.monopoly.dto;

import com.game.monopoly.model.enums.RoomMode;
import com.game.monopoly.model.enums.RoomStatus;
import com.game.monopoly.model.enums.RoomVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RoomListItemResponse {
    private Long roomId;
    private String roomCode;
    private String name;
    private String hostName;
    private RoomMode mode;
    private Integer currentPlayers;
    private Integer maxPlayers;
    private RoomVisibility visibility;
    private RoomStatus status;
}
