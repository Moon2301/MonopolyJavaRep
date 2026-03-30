package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StartBotGameResponse {
    private Long gameId;
    private String redirectUrl;
    private String difficulty;
    private Integer botCount;
}
