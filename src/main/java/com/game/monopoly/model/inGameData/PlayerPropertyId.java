package com.game.monopoly.model.inGameData;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerPropertyId implements Serializable {

    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "cell_id")
    private Integer cellId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerPropertyId)) return false;
        PlayerPropertyId that = (PlayerPropertyId) o;
        return Objects.equals(gameId, that.gameId) &&
               Objects.equals(cellId, that.cellId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, cellId);
    }
}
