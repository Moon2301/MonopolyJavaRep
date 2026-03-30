package com.game.monopoly.repository;

import com.game.monopoly.model.inGameData.InGamePlayerCardHand;
import com.game.monopoly.model.inGameData.InGamePlayerCardHandId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InGamePlayerCardHandRepository extends JpaRepository<InGamePlayerCardHand, InGamePlayerCardHandId> {
    List<InGamePlayerCardHand> findByGamePlayer_GamePlayerId(Long gamePlayerId);
}
