package com.game.monopoly.repository;

import com.game.monopoly.model.inGameData.RoomPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, Long> {
    List<RoomPlayer> findByRoom_RoomIdOrderBySlotIndexAsc(Long roomId);
    List<RoomPlayer> findAllByRoom_RoomId(Long roomId);
    Optional<RoomPlayer> findByRoom_RoomIdAndAccount_AccountId(Long roomId, Long accountId);
    long countByRoom_RoomId(Long roomId);
    void deleteByRoom_RoomIdAndAccount_AccountId(Long roomId, Long accountId);
}
