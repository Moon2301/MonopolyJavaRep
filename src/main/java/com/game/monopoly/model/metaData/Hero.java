package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Hero")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Hero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_id")
    private Integer characterId;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String rarity;

    @Column(name = "base_hp")
    private Integer baseHp = 0;

    @Column(name = "base_income_bonus")
    private Integer baseIncomeBonus = 0;

    @Column(name = "price")
    private Integer price = 0;

    @Column(name = "appearance_description", length = 300)
    private String appearanceDescription;

    @Column(name = "starting_gold_bonus")
    private Integer startingGoldBonus = 0;

    @Column(name = "default_unlocked")
    private Boolean defaultUnlocked = false;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
