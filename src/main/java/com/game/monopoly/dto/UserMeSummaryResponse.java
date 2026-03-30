package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserMeSummaryResponse {

    private String username;
    private String avatarUrl;
    private Long coins;
    private Long tickets;
    private String rankTier;

    private Integer matches;
    private Integer winRate;
    private Long totalWonAssets;

    /** Hero mặc định đã lưu trong DB (current hero). */
    private Integer currentHeroId;

    /** Nhân vật hiển thị hồ sơ (suy ra từ current hero + fallback). */
    private Integer equippedCharacterId;
    private String equippedCharacterName;
    private String equippedCharacterImageUrl;
}

