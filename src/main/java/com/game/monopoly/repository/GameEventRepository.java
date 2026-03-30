package com.game.monopoly.repository;

import com.game.monopoly.model.inGameData.GameEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameEventRepository extends JpaRepository<GameEvent, Long> {
    List<GameEvent> findByGame_GameIdOrderByCreatedAtAsc(Long gameId);
}
