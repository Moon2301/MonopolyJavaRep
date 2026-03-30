package com.game.monopoly.model.inGameData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Card")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Integer cardId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "effect_type", nullable = false, length = 50)
    private String effectType;

    @Column(name = "effect_value")
    private Integer effectValue;

    @Column(name = "effect_data", columnDefinition = "json")
    private String effectData;

    @Column(length = 50)
    private String rarity = "COMMON";

    @Column(name = "is_active")
    private Boolean isActive = true;
}
