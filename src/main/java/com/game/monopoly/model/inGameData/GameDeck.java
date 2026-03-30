package com.game.monopoly.model.inGameData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GameDeck")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameDeck {

    @EmbeddedId
    private GameDeckId id = new GameDeckId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId")
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    public GameDeck(Game game, Card card, String zone, Integer position) {
        this.game = game;
        this.card = card;
        this.id = new GameDeckId(game.getGameId(), zone, position);
    }
}
