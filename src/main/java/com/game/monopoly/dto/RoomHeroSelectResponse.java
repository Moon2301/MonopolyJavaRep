package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RoomHeroSelectResponse {
    private Long playerId;
    private Integer selectedHeroId;
    private String selectedHeroName;
}
