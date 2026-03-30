package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.Hero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HeroRepository extends JpaRepository<Hero, Integer> {
    Optional<Hero> findByName(String name);

    Optional<Hero> findFirstByDefaultUnlockedTrueOrderByCharacterIdAsc();
}
