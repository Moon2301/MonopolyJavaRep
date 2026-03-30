package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Skill")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "skill_id")
    private Integer skillId;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @Column(name = "trigger_type", nullable = false, length = 50)
    private String triggerType;

    @Column(name = "effect_type", nullable = false, length = 50)
    private String effectType;

    private Integer cooldown = 0;

    @Column(name = "effect_value")
    private Integer effectValue;

    @Column(name = "effect_formula")
    private String effectFormula;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
