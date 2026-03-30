package com.game.monopoly.dto;

import com.game.monopoly.model.enums.RoomMode;
import com.game.monopoly.model.enums.RoomVisibility;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomSettingsUpdateRequest {
    private RoomVisibility visibility;
    private String password;
    private RoomMode mode;
    private Integer maxPlayers;
}
