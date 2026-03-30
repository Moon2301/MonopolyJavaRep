package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.BoardCell;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCellRepository extends JpaRepository<BoardCell, Integer> {
}