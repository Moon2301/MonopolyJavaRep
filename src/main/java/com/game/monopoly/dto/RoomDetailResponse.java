package com.game.monopoly.dto;

import com.game.monopoly.model.enums.RoomMode;
import com.game.monopoly.model.enums.RoomStatus;
import com.game.monopoly.model.enums.RoomVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class RoomDetailResponse {

    private RoomDto room;
    private CurrentPlayerDto currentPlayer;
    private List<PlayerDto> players;
    /** Bạn đã kết bạn (ACCEPTED) và chưa có trong phòng — để mời qua tin nhắn. */
    private List<InviteFriendDto> inviteFriends;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RoomDto {
        private Long roomId;
        private String roomCode;
        private String name;
        private RoomVisibility visibility;
        private RoomMode mode;
        private Integer maxPlayers;
        private RoomStatus status;
        private Long hostPlayerId;
        /** Có khi phòng đã vào ván ({@code IN_GAME}). */
        private Long activeGameId;
        private Boolean isStarted;
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
    public static class PlayerDto {
        private Long playerId;
        private String username;
        private String avatarUrl;
        private String selectedHeroName;
        private Long selectedHeroId;
        private Boolean isHost;
        private Boolean isReady;
        private Integer slotIndex;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class InviteFriendDto {
        private Long userProfileId;
        private String username;
        private String avatarUrl;
        private String presenceStatus;
    }
}
