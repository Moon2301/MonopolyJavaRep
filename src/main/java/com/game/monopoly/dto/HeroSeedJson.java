package com.game.monopoly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Bản ghi trong {@code seed/heroes-seed.json} — seed nhân vật (Hero).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeroSeedJson {
    private String name;
    private String rarity;
    private Integer baseHp;
    private Integer baseIncomeBonus;
    private Integer price;
    private String appearanceDescription;
    private Integer startingGoldBonus;
    private Boolean defaultUnlocked;
    private Boolean isActive;
    /** Tên skill đã seed trong DB — dùng để gắn CharacterSkill. */
    private String skillName;
}
