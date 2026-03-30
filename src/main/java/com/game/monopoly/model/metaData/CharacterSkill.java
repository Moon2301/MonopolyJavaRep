package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CharacterSkill")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSkill {

    @EmbeddedId
    private CharacterSkillId id = new CharacterSkillId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("characterId")
    @JoinColumn(name = "character_id")
    private Hero hero;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "unlock_level")
    private Integer unlockLevel = 1;

    public CharacterSkill(Hero hero, Skill skill, Integer unlockLevel) {
        this.hero = hero;
        this.skill = skill;
        this.id = new CharacterSkillId(hero.getCharacterId(), skill.getSkillId());
        this.unlockLevel = unlockLevel;
    }
}
