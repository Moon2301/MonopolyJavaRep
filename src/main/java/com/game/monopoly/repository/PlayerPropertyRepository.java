package com.game.monopoly.repository;

import com.game.monopoly.model.inGameData.PlayerProperty;
import com.game.monopoly.model.inGameData.PlayerPropertyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerPropertyRepository extends JpaRepository<PlayerProperty, PlayerPropertyId> {
    List<PlayerProperty> findByGame_GameId(Long gameId);

    List<PlayerProperty> findByGame_GameIdAndOwnerPlayer_GamePlayerId(Long gameId, Long ownerGamePlayerId);

    java.util.Optional<PlayerProperty> findByGame_GameIdAndBoardCell_CellId(Long gameId, Integer cellId);

    @Query(
            "SELECT pp FROM PlayerProperty pp JOIN FETCH pp.ownerPlayer WHERE pp.id.gameId = :gameId"
                    + " AND pp.boardCell.cellId = :cellId")
    Optional<PlayerProperty> findByGameIdAndCellIdWithOwner(
            @Param("gameId") Long gameId, @Param("cellId") Integer cellId);
}
