package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.CharacterSkill;
import com.game.monopoly.model.metaData.CharacterSkillId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterSkillRepository extends JpaRepository<CharacterSkill, CharacterSkillId> {
    @EntityGraph(attributePaths = {"skill"})
    List<CharacterSkill> findByHero_CharacterId(Integer characterId);
}
