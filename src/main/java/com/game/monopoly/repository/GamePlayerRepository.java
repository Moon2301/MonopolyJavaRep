package com.game.monopoly.repository;

import com.game.monopoly.model.inGameData.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {

    List<GamePlayer> findByGameId(Long gameId);

    Optional<GamePlayer> findByGameIdAndUserProfileId(Long gameId, Long userProfileId);

    List<GamePlayer> findByUserProfileId(Long userProfileId);

    List<GamePlayer> findByGameIdOrderByTurnOrderAsc(Long gameId);

    java.util.Optional<GamePlayer> findByGameIdAndTurnOrder(Long gameId, Integer turnOrder);

}