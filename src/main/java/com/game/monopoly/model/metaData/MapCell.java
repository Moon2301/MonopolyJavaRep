package com.game.monopoly.model.metaData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "MapCell")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class MapCell {

    @EmbeddedId
    private MapCellId id = new MapCellId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mapId")
    @JoinColumn(name = "map_id")
    private Map map;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cell_id", nullable = false)
    private BoardCell boardCell;

    // Optional convenience constructor
    public MapCell(Map map, Integer position, BoardCell boardCell) {
        this.map = map;
        this.boardCell = boardCell;
        this.id = new MapCellId(map.getMapId(), position);
    }
}
