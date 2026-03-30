package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HeroPurchaseResponse {
    private Integer heroId;
    private Long remainingCoins;
    private boolean purchased;
    private String message;
}
