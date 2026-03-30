package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PrivateTableResponse {

    private RoomDto room;
    private CurrentPlayerDto currentPlayer;
    private List<PlayerSlotDto> players;
    private List<FriendDto> friends;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RoomDto {
        private Long roomId;
        private String roomCode;
        private Boolean isPrivate;
        private String mode;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class HeroDto {
        private Long heroId;
        private String name;
        private String imageUrl;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CurrentPlayerDto {
        private Long playerId;
        private String username;
        private String avatarUrl;
        private Long coins;
        private Long tickets;
        private HeroDto selectedHero;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PlayerSlotDto {
        private Long playerId;
        private String username;
        private String avatarUrl;
        private String selectedHeroName;
        private Boolean isHost;
        private Boolean isReady;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FriendDto {
        private Long friendId;
        private String username;
        private String avatarUrl;
        private Boolean isOnline;
    }
}
