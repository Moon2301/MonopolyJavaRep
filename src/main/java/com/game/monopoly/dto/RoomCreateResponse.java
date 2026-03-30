package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RoomCreateResponse {
    private Long roomId;
    private String roomCode;
    private String redirectUrl;
}
