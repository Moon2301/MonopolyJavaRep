package com.game.monopoly.model.inGameData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "InGamePlayerCardHand")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class InGamePlayerCardHand {

    @EmbeddedId
    private InGamePlayerCardHandId id = new InGamePlayerCardHandId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gamePlayerId")
    @JoinColumn(name = "game_player_id")
    private GamePlayer gamePlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cardId")
    @JoinColumn(name = "card_id")
    private Card card;

    public InGamePlayerCardHand(GamePlayer gamePlayer, Card card, Integer acquiredTurn) {
        this.gamePlayer = gamePlayer;
        this.card = card;
        this.id = new InGamePlayerCardHandId(gamePlayer.getGamePlayerId(), card.getCardId(), acquiredTurn);
    }
}