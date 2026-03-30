package com.game.monopoly.model.metaData;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class MapCellId implements Serializable {

    @Column(name = "map_id")
    private Integer mapId;

    @Column(name = "position")
    private Integer position;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapCellId)) return false;
        MapCellId that = (MapCellId) o;
        return Objects.equals(mapId, that.mapId) &&
               Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapId, position);
    }
}
