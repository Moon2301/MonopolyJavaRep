package com.game.monopoly.model.inGameData;

import com.game.monopoly.model.metaData.BoardCell;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PlayerProperty")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerProperty {

    @EmbeddedId
    private PlayerPropertyId id = new PlayerPropertyId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId")
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cellId")
    @JoinColumn(name = "cell_id")
    private BoardCell boardCell;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_player_id")
    private GamePlayer ownerPlayer;

    @Column(name = "house_level")
    private Integer houseLevel = 0;

    /** Tổng tiền đã bỏ để nâng cấp (để tính giá nhà = giá gốc + số này). */
    @Column(name = "upgrade_spent_total")
    private Long upgradeSpentTotal = 0L;

    public PlayerProperty(Game game, BoardCell boardCell, GamePlayer ownerPlayer, Integer houseLevel) {
        this.game = game;
        this.boardCell = boardCell;
        this.ownerPlayer = ownerPlayer;
        this.houseLevel = houseLevel;
        this.upgradeSpentTotal = 0L;
        this.id = new PlayerPropertyId(game.getGameId(), boardCell.getCellId());
    }
}