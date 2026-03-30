package com.game.monopoly.service;

import com.game.monopoly.dto.PrivateTableResponse;
import com.game.monopoly.dto.RoomDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrivateTableService {

    private final RoomService roomService;

    public PrivateTableResponse getPrivateTable(Long roomId, Long accountId) {
        RoomDetailResponse roomDetail = roomService.getRoomDetail(roomId, accountId);

        return PrivateTableResponse.builder()
                .room(PrivateTableResponse.RoomDto.builder()
                        .roomId(roomDetail.getRoom().getRoomId())
                        .roomCode(roomDetail.getRoom().getRoomCode())
                        .isPrivate(roomDetail.getRoom().getVisibility() != null
                                && roomDetail.getRoom().getVisibility().name().equals("PRIVATE"))
                        .mode(roomDetail.getRoom().getMode() != null ? roomDetail.getRoom().getMode().name() : null)
                        .build())
                .currentPlayer(PrivateTableResponse.CurrentPlayerDto.builder()
                        .playerId(roomDetail.getCurrentPlayer().getPlayerId())
                        .username(roomDetail.getCurrentPlayer().getUsername())
                        .avatarUrl(roomDetail.getCurrentPlayer().getAvatarUrl())
                        .coins(roomDetail.getCurrentPlayer().getCoins())
                        .tickets(roomDetail.getCurrentPlayer().getTickets())
                        .selectedHero(roomDetail.getCurrentPlayer().getSelectedHero() == null ? null
                                : PrivateTableResponse.HeroDto.builder()
                                .heroId(roomDetail.getCurrentPlayer().getSelectedHero().getHeroId())
                                .name(roomDetail.getCurrentPlayer().getSelectedHero().getName())
                                .imageUrl(roomDetail.getCurrentPlayer().getSelectedHero().getImageUrl())
                                .build())
                        .build())
                .players(roomDetail.getPlayers().stream()
                        .map(player -> PrivateTableResponse.PlayerSlotDto.builder()
                                .playerId(player.getPlayerId())
                                .username(player.getUsername())
                                .avatarUrl(player.getAvatarUrl())
                                .selectedHeroName(player.getSelectedHeroName())
                                .isHost(player.getIsHost())
                                .isReady(player.getIsReady())
                                .build())
                        .toList())
                .friends(java.util.List.of())
                .build();
    }
}
