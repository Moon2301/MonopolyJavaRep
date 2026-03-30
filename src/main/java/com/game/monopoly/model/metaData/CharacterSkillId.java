package com.game.monopoly.model.metaData;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSkillId implements Serializable {

    @Column(name = "character_id")
    private Integer characterId;

    @Column(name = "skill_id")
    private Integer skillId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CharacterSkillId)) return false;
        CharacterSkillId that = (CharacterSkillId) o;
        return Objects.equals(characterId, that.characterId) &&
               Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterId, skillId);
    }
}
