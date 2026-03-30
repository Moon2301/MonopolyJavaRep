package com.game.monopoly.repository;

import com.game.monopoly.model.inGameData.GameDeck;
import com.game.monopoly.model.inGameData.GameDeckId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameDeckRepository extends JpaRepository<GameDeck, GameDeckId> {
    List<GameDeck> findByGame_GameIdAndId_ZoneOrderById_PositionAsc(Long gameId, String zone);
}
