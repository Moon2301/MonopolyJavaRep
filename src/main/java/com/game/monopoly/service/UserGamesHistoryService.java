package com.game.monopoly.service;

import com.game.monopoly.dto.GameHistoryItemDto;
import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.inGameData.Game;
import com.game.monopoly.model.inGameData.GamePlayer;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.GamePlayerRepository;
import com.game.monopoly.repository.GameRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserGamesHistoryService {

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UserProfileRepository userProfileRepository;
    private final AccountRepository accountRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final GameRepository gameRepository;

    public List<GameHistoryItemDto> getHistory(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        UserProfile profile = userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));

        Long userProfileId = profile.getUserProfileId();

        List<GamePlayer> userGamePlayers = gamePlayerRepository.findByUserProfileId(userProfileId);

        if (userGamePlayers.isEmpty()) {
            return List.of();
        }

        Map<Long, GamePlayer> gamePlayerByGameId = userGamePlayers.stream()
                .collect(Collectors.toMap(GamePlayer::getGameId, gp -> gp, (a, b) -> a));

        List<Game> finishedGames = gamePlayerByGameId.keySet().stream()
                .map(gameId -> gameRepository.findById(gameId).orElse(null))
                .filter(Objects::nonNull)
                .filter(game -> game.getStatus() == GameStatus.FINISHED)
                .sorted(gameSortComparator())
                .collect(Collectors.toList());

        if (finishedGames.isEmpty()) {
            return List.of();
        }

        return finishedGames.stream()
                .map(game -> toDto(gamePlayerByGameId.get(game.getGameId()), game))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private GameHistoryItemDto toDto(GamePlayer gamePlayer, Game game) {
        if (gamePlayer == null) {
            return null;
        }

        LocalDateTime startedAt = game.getStartedAt();
        LocalDateTime endedAt = game.getEndedAt();

        String result;
        Long winnerPlayerId = game.getWinnerPlayerId();
        if (winnerPlayerId == null) {
            result = "DRAW";
        } else if (Objects.equals(winnerPlayerId, gamePlayer.getGamePlayerId())) {
            result = "WIN";
        } else {
            result = "LOSE";
        }

        return GameHistoryItemDto.builder()
                .gameId(game.getGameId())
                .mapId(game.getMapId())
                .startedAt(startedAt != null ? startedAt.toString() : null)
                .endedAt(endedAt != null ? endedAt.toString() : null)
                .startedAtFormatted(startedAt != null ? startedAt.format(DISPLAY_FORMATTER) : null)
                .endedAtFormatted(endedAt != null ? endedAt.format(DISPLAY_FORMATTER) : null)
                .result(result)
                .yourCharacterId(gamePlayer.getCharacterId())
                .build();
    }

    private Comparator<Game> gameSortComparator() {
        return (a, b) -> {
            LocalDateTime aTs = a.getEndedAt() != null ? a.getEndedAt() : a.getStartedAt();
            LocalDateTime bTs = b.getEndedAt() != null ? b.getEndedAt() : b.getStartedAt();

            if (aTs == null && bTs == null) {
                return 0;
            }
            if (aTs == null) {
                return 1;
            }
            if (bTs == null) {
                return -1;
            }
            return bTs.compareTo(aTs);
        };
    }

}

