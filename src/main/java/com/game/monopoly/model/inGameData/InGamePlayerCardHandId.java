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
public class InGamePlayerCardHandId implements Serializable {

    @Column(name = "game_player_id")
    private Long gamePlayerId;

    @Column(name = "card_id")
    private Integer cardId;

    @Column(name = "acquired_turn")
    private Integer acquiredTurn;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InGamePlayerCardHandId)) return false;
        InGamePlayerCardHandId that = (InGamePlayerCardHandId) o;
        return Objects.equals(gamePlayerId, that.gamePlayerId) &&
               Objects.equals(cardId, that.cardId) &&
               Objects.equals(acquiredTurn, that.acquiredTurn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gamePlayerId, cardId, acquiredTurn);
    }
}
