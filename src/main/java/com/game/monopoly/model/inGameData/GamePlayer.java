package com.game.monopoly.model.inGameData;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GamePlayer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_player_id")
    private Long gamePlayerId;

    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "user_profile_id")
    private Long userProfileId;

    @Column(name = "character_id")
    private Integer characterId;

    private Integer turnOrder;
    private Long balance;
    private Integer position;

    private Boolean isBankrupt;
    private Boolean isBot;

    /** Đang ở tù (không đi được cho đến khi đủ điều kiện ra). */
    @Builder.Default
    @Column(name = "in_jail")
    private Boolean inJail = false;

    /**
     * Số lần lắc trong tù không ra đôi. Đủ 3 → lượt sau có thể ra tù theo rule (bất kỳ tổng xx).
     */
    @Builder.Default
    @Column(name = "jail_failed_rolls")
    private Integer jailFailedRolls = 0;

    /** Chuỗi xx liên tiếp trong cùng lượt; lần thứ 3 → vào tù. */
    @Builder.Default
    @Column(name = "consecutive_doubles")
    private Integer consecutiveDoubles = 0;

    /** Hồi chiêu skill (lượt còn lại), 0 = sẵn sàng. */
    @Builder.Default
    @Column(name = "skill_cooldown_remaining")
    private Integer skillCooldownRemaining = 0;

    /** Thứ tự bị loại (1 = ra đầu tiên); người thắng = null. */
    @Column(name = "elimination_order")
    private Integer eliminationOrder;

    /** Skill Điều Khoản Vàng: ô đánh dấu (cellId) để mua lại theo {@link #skillBuybackPercent}. */
    @Column(name = "skill_marked_cell_id")
    private Integer skillMarkedCellId;

    /** Phần trăm giá niêm yết khi mua lại ô đã đánh dấu (vd 100 = đúng giá gốc). */
    @Column(name = "skill_buyback_percent")
    private Integer skillBuybackPercent;

    /**
     * Xu thưởng (10–200) cộng vào tài khoản khi ván kết thúc — hiển thị thay số dư trong ván ở màn kết quả.
     */
    @Column(name = "end_match_coin_reward")
    private Integer endMatchCoinReward;
}