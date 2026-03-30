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
public class GameDeckId implements Serializable {

    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "zone", length = 50)
    private String zone;

    @Column(name = "position")
    private Integer position;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameDeckId)) return false;
        GameDeckId that = (GameDeckId) o;
        return Objects.equals(gameId, that.gameId) &&
               Objects.equals(zone, that.zone) &&
               Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, zone, position);
    }
}
