package com.game.monopoly.repository;

import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.inGameData.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {

    long countByStatusIn(Collection<GameStatus> statuses);

    @Query(
            """
            SELECT DISTINCT g FROM Game g
            INNER JOIN GamePlayer gp ON gp.gameId = g.gameId
            WHERE g.status = :playing
            AND gp.userProfileId = :userProfileId
            AND (gp.isBot IS NULL OR gp.isBot = false)
            """)
    List<Game> findPlayingGamesForHumanProfile(
            @Param("userProfileId") Long userProfileId, @Param("playing") GameStatus playing);
}