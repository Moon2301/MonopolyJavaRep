package com.game.monopoly.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BotSlotRequest {
    /** easy | hard */
    private String difficulty;
    private Integer heroId;
}
