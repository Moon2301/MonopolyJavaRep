package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HeroOptionResponse {
    private Integer heroId;
    private String name;
    private String rarity;
    private String imageUrl;
    private Integer price;
    private String appearanceDescription;
    private String skillName;
    private String skillDescription;
    private Integer skillCooldown;
    private Boolean defaultUnlocked;
}
