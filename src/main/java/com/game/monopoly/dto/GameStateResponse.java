package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class GameStateResponse {
    private Long gameId;
    private String status;
    private Integer currentTurn;
    private Integer currentPlayerOrder;
    private String turnState;
    private Integer lastDice1;
    private Integer lastDice2;
    /** Giây còn lại của lượt người (null nếu không phải lượt người hoặc game kết thúc). */
    private Integer turnSecondsRemaining;
    /** Có phải lượt của tài khoản gửi X-Account-Id không (null nếu không gửi header). */
    private Boolean myTurn;
    /** Thứ tự lượt (1..n) của người gửi X-Account-Id trong ván; null nếu không tham gia hoặc không gửi header. */
    private Integer myPlayerTurnOrder;
    private Integer totalBoardCells;
    private CellInfoDto currentCell;
    private List<PlayerStateDto> players;
    /** Ô đã có chủ: chỉ số ô trên bàn (0..n-1) + lượt người chơi (1..4). */
    private List<OwnedCellDto> ownedCells;
    /**
     * Thông báo thuê đất vừa xảy ra (chỉ gửi một lần rồi xóa khỏi hàng đợi khi client gọi getState).
     */
    private List<RentNoticeDto> rentNotices;
    /**
     * Dòng log ván (lắc xúc xắc, mua, nâng cấp, kết lượt…) — một lần rồi xóa như rentNotices.
     */
    private List<String> gameLogLines;
    /** Khi không đủ tiền trả thuê — cần bán tài sản hoặc phá sản. */
    private DebtSituationDto debtSituation;
    /** Khi ván kết thúc — xếp hạng theo thứ tự bị loại (hạng 1 = thắng). */
    private List<FinalRankingEntryDto> finalRanking;
    /**
     * Đứng trên đất đối thủ, đủ tiền mua lại 130% — chưa trả thuê; cần chọn trả thuê hoặc mua lại.
     */
    private OpponentLandPendingDto opponentLandPending;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OpponentLandPendingDto {
        private Long rentAmount;
        private Long buybackPrice;
        /** Phần trăm giá niêm yết áp dụng cho mua lại (vd 130 hoặc 100). */
        private Integer buybackPercent;
        private String cellName;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DebtSituationDto {
        private long amountOwed;
        private Integer creditorTurnOrder;
        private String creditorName;
        private String causeCellName;
        private List<DebtAssetDto> assets;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DebtAssetDto {
        private Integer cellId;
        private String name;
        private Integer boardIndex;
        private Integer houseLevel;
        /** SELL_HOUSE hoặc MORTGAGE */
        private String suggestedAction;
        private Long cashIfSold;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RentNoticeDto {
        private String payerName;
        private long amountPaid;
        private String cellName;
        private String ownerName;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OwnedCellDto {
        private Integer boardIndex;
        private Integer ownerTurnOrder;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CellInfoDto {
        private Integer cellId;
        private String name;
        private String type;
        private Long price;
        /** Giá gốc ô + tổng tiền đã nâng cấp. */
        private Long houseValue;
        private Long upgradeCost;
        private Long estimatedRent;
        private Long ownerGamePlayerId;
        private Integer ownerTurnOrder;
        private Integer houseLevel;
        /** Ô có thể mua (đất / RR / tiện ích) — false với ô sự kiện, thuế, tù, xuất phát… */
        private Boolean purchasable;
        private Boolean canBuy;
        private Boolean canUpgrade;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PlayerStateDto {
        private Long gamePlayerId;
        private Long userProfileId;
        private Integer turnOrder;
        private Integer position;
        private Long balance;
        private Boolean isBot;
        private Boolean isBankrupt;
        /** Tên hiển thị (username hoặc Bot). */
        private String username;
        /** Ảnh đại diện tài khoản — dùng sảnh / thẻ người chơi. */
        private String avatarUrl;
        /** Ảnh nhân vật (hero) — ưu tiên hiển thị trên quân cờ bàn. */
        private String heroImageUrl;
        /** Tên hero — dùng với HeroSystem SVG khi không có ảnh. */
        private String heroName;
        private Boolean inJail;
        private Integer jailFailedRolls;
        private List<PlayerSkillDto> skills;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FinalRankingEntryDto {
        /** 1 = thắng, 2 = á quân, … */
        private Integer rank;
        private Integer turnOrder;
        private String displayName;
        /** Tên hero trong trận (nếu có). */
        private String heroName;
        /** Số dư trong ván (khi không có {@link #coinReward}). */
        private Long balance;
        /**
         * Xu cộng vào tài khoản khi hết ván (10–200); ưu tiên hiển thị thay {@link #balance}.
         */
        private Long coinReward;
        private Boolean isBot;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PlayerSkillDto {
        private Integer skillId;
        private String name;
        private String description;
        /** EffectType từ DB — client dùng để biết skill cần chọn ô. */
        private String effectType;
        /** Cần gửi targetCellId khi kích hoạt. */
        private Boolean requiresTarget;
        /** Kỹ năng chọn cặp xúc xắc (DUEL_DICE): gửi dice1, dice2 khi kích hoạt. */
        private Boolean requiresDiceChoice;
        /** PASSIVE hoặc ACTIVE */
        private String triggerType;
        private Integer cooldownTurns;
        /** Lượt hồi còn lại (0 = sẵn sàng). */
        private Integer cooldownRemaining;
        /** Skill thụ động: luôn hiệu lực trong trận. */
        private Boolean passiveActive;
        /** Skill chủ động: có thể kích hoạt ngay (hết hồi). */
        private Boolean readyToActivate;
    }
}
