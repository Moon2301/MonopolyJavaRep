package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ActiveGameResponse {
    private boolean hasActiveGame;
    private Long gameId;
    private Long roomId;
    private String roomCode;
    private boolean soloVsAi;
}
