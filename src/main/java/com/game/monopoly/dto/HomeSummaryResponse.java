package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HomeSummaryResponse {

    private PlayerDto player;
    private OnlineStatsDto onlineStats;
    private TournamentDto tournament;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PlayerDto {
        private String username;
        private String avatarUrl;
        private Long coins;
        private Long tickets;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OnlineStatsDto {
        private Integer playersOnline;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class TournamentDto {
        private String title;
        private String endsAt;
    }
}
