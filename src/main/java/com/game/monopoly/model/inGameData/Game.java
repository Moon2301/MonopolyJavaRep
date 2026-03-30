package com.game.monopoly.model.inGameData;

import com.game.monopoly.model.enums.GameStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Game")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "map_id")
    private Integer mapId;

    @Column(name = "created_by")
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    private Integer maxPlayers;
    private Integer currentTurn;
    private Integer currentPlayerOrder;

    private String turnState;

    private Integer version;

    private Long winnerPlayerId;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    /** Mốc bắt đầu đếm thời gian cho lượt người (chờ tung xúc xắc / hành động sau khi đi). */
    @Column(name = "human_turn_started_at")
    private LocalDateTime humanTurnStartedAt;

    /**
     * Chỉ người vs bot (không gắn phòng) — không thưởng xu khi kết thúc, và quy tắc đầu hàng riêng.
     */
    @Column(name = "solo_vs_ai")
    @Builder.Default
    private Boolean soloVsAi = false;

    /** Tiền thuê đang nợ (chưa trả đủ) — khi turnState = INSOLVENT. */
    @Column(name = "debt_rent_amount")
    private Long debtRentAmount;

    @Column(name = "debt_creditor_game_player_id")
    private Long debtCreditorGamePlayerId;

    @Column(name = "debt_cell_id")
    private Integer debtCellId;

    /** Tăng mỗi khi có người bị loại — gán {@link GamePlayer#getEliminationOrder()}. */
    @Column(name = "elimination_sequence")
    private Integer eliminationSequence;
}