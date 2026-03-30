package com.game.monopoly.repository;

import com.game.monopoly.model.metaData.MapCell;
import com.game.monopoly.model.metaData.MapCellId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MapCellRepository extends JpaRepository<MapCell, MapCellId> {

    List<MapCell> findByMap_MapIdOrderById_PositionAsc(Integer mapId);

    @Query("SELECT mc FROM MapCell mc JOIN FETCH mc.boardCell WHERE mc.id.mapId = :mapId ORDER BY mc.id.position ASC")
    List<MapCell> findAllByMapIdOrderByPositionWithCell(@Param("mapId") Integer mapId);
}