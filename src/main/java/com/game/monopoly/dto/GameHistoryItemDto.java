package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class GameHistoryItemDto {

    private Long gameId;
    private Integer mapId;

    private String startedAt;
    private String endedAt;
    private String startedAtFormatted;
    private String endedAtFormatted;

    private String result; // WIN / LOSE / DRAW
    private Integer yourCharacterId;
}

