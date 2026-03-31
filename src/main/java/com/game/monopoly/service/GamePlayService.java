package com.game.monopoly.service;

import com.game.monopoly.dto.DebtSellRequest;
import com.game.monopoly.dto.GameActionResponse;
import com.game.monopoly.dto.GameStateResponse;
import com.game.monopoly.dto.SkillActivateRequest;
import com.game.monopoly.dto.BotSlotRequest;
import com.game.monopoly.dto.StartBotGameRequest;
import com.game.monopoly.dto.StartBotGameResponse;
import com.game.monopoly.MonopolyGameRules;
import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.enums.RoomStatus;
import com.game.monopoly.model.inGameData.Game;
import com.game.monopoly.model.inGameData.GamePlayer;
import com.game.monopoly.model.inGameData.RoomPlayer;
import com.game.monopoly.model.inGameData.PlayerProperty;
import com.game.monopoly.model.inGameData.PlayerPropertyId;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.BoardCell;
import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.model.metaData.MapCell;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.*;
import com.game.monopoly.service.skill.PlayerSkillViewService;
import com.game.monopoly.service.skill.SkillActivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GamePlayService {

    private static final long UPGRADE_BASE_COST = 100L;
    /** Khi bán/thế chấp để trả nợ: chỉ nhận % giá trị danh nghĩa (ví dụ 90). */
    private static final int LIQUIDATION_PERCENT = 90;
    private static final int HUMAN_WAIT_ROLL_SECONDS = 25;
    private static final int HUMAN_ACTION_SECONDS = 30;

    private static final long DISCONNECT_SUBSTITUTE_AFTER_MS = 60_000L;

    private final GameRepository gameRepository;
    private final RoomRepository roomRepository;
    private final RoomPlayerRepository roomPlayerRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerPropertyRepository playerPropertyRepository;
    private final BoardCellRepository boardCellRepository;
    private final MapCellRepository mapCellRepository;
    private final BoardClassicMapBootstrapService boardClassicMapBootstrapService;
    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final HeroRepository heroRepository;
    private final PlayerSkillViewService playerSkillViewService;
    private final SkillActivationService skillActivationService;
    private final PresenceRegistry presenceRegistry;
    private final Random random = new Random();

    private final Map<Long, Integer[]> lastDiceByGame = new HashMap<>();
    private final Map<Long, String> botDifficultyByGame = new HashMap<>();
    /** Độ khó theo từng bot (gamePlayerId). */
    private final Map<Long, String> botDifficultyByGamePlayerId = new ConcurrentHashMap<>();
    private final Map<Long, List<GameStateResponse.RentNoticeDto>> pendingRentNotices = new ConcurrentHashMap<>();

    private final Map<Long, List<String>> pendingGameLogs = new ConcurrentHashMap<>();

    /** Đất đối thủ: đủ tiền mua lại — chờ trả thuê hoặc mua lại. */
    private static final class OpponentLandPending {
        final long rentAmount;
        final long buybackPrice;
        final int cellId;
        final long ownerGamePlayerId;
        final int buybackPercent;

        OpponentLandPending(
                long rentAmount,
                long buybackPrice,
                int cellId,
                long ownerGamePlayerId,
                int buybackPercent) {
            this.rentAmount = rentAmount;
            this.buybackPrice = buybackPrice;
            this.cellId = cellId;
            this.ownerGamePlayerId = ownerGamePlayerId;
            this.buybackPercent = buybackPercent;
        }
    }

    private final Map<Long, OpponentLandPending> pendingOpponentLandByGameId = new ConcurrentHashMap<>();
    /** Đi vào ô Chance cho thêm một lượt {@code WAIT_ROLL}. */
    private final Map<Long, Boolean> pendingChanceExtraRollByGameId = new ConcurrentHashMap<>();

    private static final String TURN_STATE_TEAMUP_INVITE = "TEAMUP_INVITE_REQUIRED";
    private static final String TURN_STATE_TEAMUP_ACCEPT = "TEAMUP_ACCEPT_REQUIRED";

    /**
     * Team-up:
     * - INVITE: creditor (người làm chủ nợ) quyết định có mời không
     * - ACCEPT: dependent (người đã bị loại) quyết định đồng ý hay không
     */
    private static final class TeamUpPending {
        final Long creditorGamePlayerId;
        final Long dependentGamePlayerId;
        final String phase; // INVITE / ACCEPT

        TeamUpPending(Long creditorGamePlayerId, Long dependentGamePlayerId, String phase) {
            this.creditorGamePlayerId = creditorGamePlayerId;
            this.dependentGamePlayerId = dependentGamePlayerId;
            this.phase = phase;
        }
    }

    private final Map<Long, TeamUpPending> pendingTeamUpByGameId = new ConcurrentHashMap<>();

    private static final NumberFormat VI_MONEY =
            NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));

    private String formatMoneyLog(long value) {
        return VI_MONEY.format(value);
    }

    private void enqueueGameLog(Long gameId, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        pendingGameLogs.compute(
                gameId,
                (k, v) -> {
                    List<String> list = v != null ? v : new ArrayList<>();
                    list.add(message);
                    return list;
                });
    }

    /**
     * Log chuyển tiền giữa hai người: (1) mô tả (2) người trả & số dư (3) người nhận & số dư; {@code receiverNote}
     * ví dụ nguồn skill.
     */
    private void logMoneyTransferThreeLines(
            Long gameId,
            String actionLine,
            GamePlayer payer,
            GamePlayer receiver,
            long amount,
            String receiverNote) {
        enqueueGameLog(gameId, actionLine);
        if (payer != null && amount > 0) {
            long pb = payer.getBalance() == null ? 0L : payer.getBalance();
            enqueueGameLog(
                    gameId,
                    displayNameForPlayer(payer)
                            + " -"
                            + formatMoneyLog(amount)
                            + " tiền, số dư: "
                            + formatMoneyLog(pb));
        }
        if (receiver != null && amount > 0) {
            long rb = receiver.getBalance() == null ? 0L : receiver.getBalance();
            String mid =
                    receiverNote != null && !receiverNote.isBlank()
                            ? " (từ "
                                    + receiverNote
                                    + ")"
                            : "";
            enqueueGameLog(
                    gameId,
                    displayNameForPlayer(receiver)
                            + " +"
                            + formatMoneyLog(amount)
                            + mid
                            + " tiền, số dư: "
                            + formatMoneyLog(rb));
        }
    }

    private void logSpendOnly(Long gameId, String actionLine, GamePlayer payer, long amount) {
        enqueueGameLog(gameId, actionLine);
        if (payer != null && amount > 0) {
            long pb = payer.getBalance() == null ? 0L : payer.getBalance();
            enqueueGameLog(
                    gameId,
                    displayNameForPlayer(payer)
                            + " -"
                            + formatMoneyLog(amount)
                            + " tiền, số dư: "
                            + formatMoneyLog(pb));
        }
    }

    private void logReceiveOnly(Long gameId, String actionLine, GamePlayer receiver, long amount, String note) {
        enqueueGameLog(gameId, actionLine);
        if (receiver != null && amount != 0) {
            long rb = receiver.getBalance() == null ? 0L : receiver.getBalance();
            String suffix = note != null && !note.isBlank() ? " (từ " + note + ")" : "";
            enqueueGameLog(
                    gameId,
                    displayNameForPlayer(receiver)
                            + " +"
                            + formatMoneyLog(amount)
                            + suffix
                            + " tiền, số dư: "
                            + formatMoneyLog(rb));
        }
    }

    private void logDiceMovement(Game game, GamePlayer player, int d1, int d2, long passGoBonus) {
        String who = displayNameForPlayer(player);
        int sum = d1 + d2;
        StringBuilder sb = new StringBuilder();
        sb.append(who)
                .append(" lắc ")
                .append(d1)
                .append(" + ")
                .append(d2)
                .append(" (tổng ")
                .append(sum)
                .append("), đi ")
                .append(sum)
                .append(" bước");
        if (passGoBonus > 0) {
            sb.append(" · Qua ô xuất phát +").append(formatMoneyLog(passGoBonus));
        }
        if (d1 == d2) {
            sb.append(" · Đôi!");
        }
        enqueueGameLog(game.getGameId(), sb.toString());
    }

    @Transactional
    public StartBotGameResponse startBotGame(Long accountId, StartBotGameRequest request) {
        UserProfile profile = getProfileByAccountId(accountId);
        List<Game> alreadyPlaying =
                gameRepository.findPlayingGamesForHumanProfile(
                        profile.getUserProfileId(), GameStatus.PLAYING);
        if (!alreadyPlaying.isEmpty()) {
            throw new RuntimeException(
                    "Bạn đang trong một ván. Hãy kết thúc hoặc quay lại ván trước.");
        }
        String legacyDifficulty = normalizeDifficulty(request != null ? request.getDifficulty() : null);

        List<BotSlotRequest> slotList =
                request != null && request.getBotSlots() != null && !request.getBotSlots().isEmpty()
                        ? request.getBotSlots().stream().limit(3).toList()
                        : null;

        int botCount;
        if (slotList != null) {
            botCount = Math.min(3, Math.max(1, slotList.size()));
        } else {
            botCount = 1;
            if (request != null && request.getBotCount() != null) {
                botCount = Math.min(3, Math.max(1, request.getBotCount()));
            }
        }

        int maxPlayers = 1 + botCount;

        Game game = Game.builder()
                .mapId(1)
                .createdBy(accountId)
                .status(GameStatus.PLAYING)
                .maxPlayers(maxPlayers)
                .currentTurn(1)
                .currentPlayerOrder(1)
                .turnState("WAIT_ROLL")
                .version(1)
                .eliminationSequence(0)
                .soloVsAi(true)
                .createdAt(LocalDateTime.now())
                .startedAt(LocalDateTime.now())
                .humanTurnStartedAt(LocalDateTime.now())
                .build();
        game = gameRepository.save(game);

        Integer heroId = request != null ? request.getHeroId() : null;
        if (heroId != null && heroRepository.findById(heroId).isEmpty()) {
            heroId = null;
        }

        GamePlayer human = GamePlayer.builder()
                .gameId(game.getGameId())
                .userProfileId(profile.getUserProfileId())
                .characterId(heroId)
                .turnOrder(1)
                .balance(MonopolyGameRules.IN_GAME_STARTING_BALANCE)
                .position(0)
                .isBankrupt(false)
                .isBot(false)
                .build();

        List<GamePlayer> toSave = new ArrayList<>();
        toSave.add(human);

        if (slotList != null) {
            for (int b = 0; b < botCount; b++) {
                BotSlotRequest slot = slotList.get(b);
                String diff = normalizeDifficulty(slot != null ? slot.getDifficulty() : null);
                Integer cid = slot != null ? slot.getHeroId() : null;
                if (cid != null && heroRepository.findById(cid).isEmpty()) {
                    cid = null;
                }
                long botBal =
                        "hard".equals(diff)
                                ? MonopolyGameRules.IN_GAME_STARTING_BALANCE + MonopolyGameRules.IN_GAME_BOT_HARD_EXTRA
                                : MonopolyGameRules.IN_GAME_STARTING_BALANCE;
                GamePlayer bot = GamePlayer.builder()
                        .gameId(game.getGameId())
                        .userProfileId(null)
                        .characterId(cid)
                        .turnOrder(2 + b)
                        .balance(botBal)
                        .position(0)
                        .isBankrupt(false)
                        .isBot(true)
                        .build();
                toSave.add(bot);
            }
        } else {
            long botStartBalance =
                    "hard".equals(legacyDifficulty)
                            ? MonopolyGameRules.IN_GAME_STARTING_BALANCE + MonopolyGameRules.IN_GAME_BOT_HARD_EXTRA
                            : MonopolyGameRules.IN_GAME_STARTING_BALANCE;
            for (int b = 1; b <= botCount; b++) {
                GamePlayer bot = GamePlayer.builder()
                        .gameId(game.getGameId())
                        .userProfileId(null)
                        .characterId(null)
                        .turnOrder(1 + b)
                        .balance(botStartBalance)
                        .position(0)
                        .isBankrupt(false)
                        .isBot(true)
                        .build();
                toSave.add(bot);
            }
        }

        List<GamePlayer> savedPlayers = new ArrayList<>(gamePlayerRepository.saveAll(toSave));

        Long gid = game.getGameId();
        if (slotList != null) {
            int slotIdx = 0;
            for (GamePlayer gp : savedPlayers) {
                if (Boolean.TRUE.equals(gp.getIsBot()) && slotIdx < slotList.size()) {
                    BotSlotRequest slot = slotList.get(slotIdx++);
                    String diff = normalizeDifficulty(slot != null ? slot.getDifficulty() : null);
                    botDifficultyByGamePlayerId.put(gp.getGamePlayerId(), diff);
                }
            }
            botDifficultyByGame.put(gid, "mixed");
        } else {
            botDifficultyByGame.put(gid, legacyDifficulty);
        }

        String redirectDiff = slotList != null ? "mixed" : legacyDifficulty;
        return StartBotGameResponse.builder()
                .gameId(gid)
                .difficulty(redirectDiff)
                .botCount(botCount)
                .redirectUrl("/game-board?gameId=" + gid + "&vsBot=1&difficulty=" + redirectDiff + "&bots=" + botCount)
                .build();
    }

    @Transactional
    public GameStateResponse getState(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        finishGameIfAtMostOneActivePlayer(game);
        game = getGame(gameId);
        healStuckInsolvencyIfNeeded(game);
        game = getGame(gameId);
        ensureHumanTurnClockStarted(game);
        resolveHumanTurnAutomation(game);
        game = getGame(gameId);
        advanceOneBotStep(game);
        game = getGame(gameId);

        List<GamePlayer> players = gamePlayerRepository.findByGameIdOrderByTurnOrderAsc(gameId);
        Integer[] dice = lastDiceByGame.getOrDefault(gameId, new Integer[]{null, null});
        GamePlayer currentPlayer = getCurrentTurnPlayer(game);
        BoardCell currentCell = getCellByPosition(game, currentPlayer.getPosition());
        Optional<PlayerProperty> currentCellProperty = playerPropertyRepository
                .findByGame_GameIdAndBoardCell_CellId(gameId, currentCell.getCellId());

        Integer turnSecondsRemaining = computeTurnSecondsRemaining(game, currentPlayer);
        Boolean myTurn = computeMyTurn(accountId, currentPlayer);
        Integer myPlayerTurnOrder = resolveMyPlayerTurnOrder(accountId, players);

        List<GameStateResponse.RentNoticeDto> rentNotices =
                Optional.ofNullable(pendingRentNotices.remove(gameId)).orElseGet(List::of);
        List<String> gameLogLines =
                Optional.ofNullable(pendingGameLogs.remove(gameId)).orElseGet(List::of);

        return GameStateResponse.builder()
                .gameId(game.getGameId())
                .status(game.getStatus().name())
                .currentTurn(game.getCurrentTurn())
                .currentPlayerOrder(game.getCurrentPlayerOrder())
                .turnState(game.getTurnState())
                .lastDice1(dice[0])
                .lastDice2(dice[1])
                .turnSecondsRemaining(turnSecondsRemaining)
                .myTurn(myTurn)
                .myPlayerTurnOrder(myPlayerTurnOrder)
                .totalBoardCells(getBoardCellCount(game))
                .currentCell(toCellInfo(currentCell, currentCellProperty.orElse(null), currentPlayer, game.getTurnState()))
                .players(players.stream().map(this::toPlayerState).toList())
                .ownedCells(buildOwnedCellsSnapshot(game))
                .rentNotices(rentNotices)
                .gameLogLines(gameLogLines)
                .debtSituation(buildDebtSituation(game, currentPlayer))
                .opponentLandPending(buildOpponentLandPending(gameId, currentCell, currentPlayer))
                .teamUpPending(buildTeamUpPending(gameId))
                .finalRanking(
                        game.getStatus() == GameStatus.FINISHED ? buildFinalRanking(game) : null)
                .build();
    }

    private GameStateResponse.OpponentLandPendingDto buildOpponentLandPending(
            Long gameId, BoardCell currentCell, GamePlayer payer) {
        OpponentLandPending p = pendingOpponentLandByGameId.get(gameId);
        if (p == null || currentCell.getCellId() == null || !Objects.equals(p.cellId, currentCell.getCellId())) {
            return null;
        }
        long rentToDisplay = applyDependentRentPenaltyIfNeeded(payer, p.rentAmount);
        return GameStateResponse.OpponentLandPendingDto.builder()
                .rentAmount(rentToDisplay)
                .buybackPrice(p.buybackPrice)
                .buybackPercent(p.buybackPercent)
                .cellName(currentCell.getName())
                .build();
    }

    private GameStateResponse.TeamUpPendingDto buildTeamUpPending(Long gameId) {
        TeamUpPending p = pendingTeamUpByGameId.get(gameId);
        if (p == null) {
            return null;
        }
        GamePlayer creditor = gamePlayerRepository.findById(p.creditorGamePlayerId).orElse(null);
        GamePlayer dependent = gamePlayerRepository.findById(p.dependentGamePlayerId).orElse(null);
        if (creditor == null || dependent == null) {
            return null;
        }
        String phase = TURN_STATE_TEAMUP_INVITE.equalsIgnoreCase(p.phase) ? "INVITE" : "ACCEPT";
        return GameStateResponse.TeamUpPendingDto.builder()
                .phase(phase)
                .creditorGamePlayerId(p.creditorGamePlayerId)
                .dependentGamePlayerId(p.dependentGamePlayerId)
                .creditorTurnOrder(creditor.getTurnOrder())
                .dependentTurnOrder(dependent.getTurnOrder())
                .creditorName(displayNameForPlayer(creditor))
                .dependentName(displayNameForPlayer(dependent))
                .build();
    }

    private boolean isTeamDependent(GamePlayer p) {
        if (p == null) {
            return false;
        }
        Long ownerId = p.getTeamOwnerGamePlayerId();
        return ownerId != null && !Objects.equals(ownerId, p.getGamePlayerId());
    }

    private Long resolveTeamOwnerGamePlayerId(GamePlayer p) {
        if (p == null) {
            return null;
        }
        return p.getTeamOwnerGamePlayerId() != null ? p.getTeamOwnerGamePlayerId() : p.getGamePlayerId();
    }

    private GamePlayer resolveTeamOwner(GamePlayer actor) {
        if (actor == null) {
            return null;
        }
        Long ownerId = resolveTeamOwnerGamePlayerId(actor);
        if (ownerId == null) {
            return actor;
        }
        if (Objects.equals(ownerId, actor.getGamePlayerId())) {
            return actor;
        }
        return gamePlayerRepository.findById(ownerId).orElse(actor);
    }

    private GamePlayer resolveTeamPartner(GamePlayer actor) {
        if (actor == null) {
            return null;
        }
        Long partnerId = actor.getTeamPartnerGamePlayerId();
        if (partnerId == null || Objects.equals(partnerId, actor.getGamePlayerId())) {
            return null;
        }
        return gamePlayerRepository.findById(partnerId).orElse(null);
    }

    private long getTeamBalance(GamePlayer actor) {
        GamePlayer owner = resolveTeamOwner(actor);
        return owner == null || owner.getBalance() == null ? 0L : owner.getBalance();
    }

    /**
     * Đồng bộ balance dùng chung của team (owner + phụ thuộc).
     *
     * Lưu ý: gọi phương thức này sẽ persist luôn vào DB để tránh lệch state giữa các nhánh.
     */
    private void setTeamBalance(GamePlayer actor, long newBalance) {
        if (actor == null) {
            return;
        }
        GamePlayer owner = resolveTeamOwner(actor);
        GamePlayer partner = resolveTeamPartner(actor);

        // Set local trước
        actor.setBalance(newBalance);
        if (owner != null) {
            owner.setBalance(newBalance);
        }
        if (partner != null) {
            partner.setBalance(newBalance);
        }

        // Save distinct
        if (owner != null && !Objects.equals(owner.getGamePlayerId(), actor.getGamePlayerId())) {
            gamePlayerRepository.save(owner);
        }
        if (partner != null
                && !Objects.equals(partner.getGamePlayerId(), actor.getGamePlayerId())
                && (owner == null || !Objects.equals(partner.getGamePlayerId(), owner.getGamePlayerId()))) {
            gamePlayerRepository.save(partner);
        }
        gamePlayerRepository.save(actor);
    }

    private long applyDependentRentPenaltyIfNeeded(GamePlayer payer, long baseRent) {
        if (isTeamDependent(payer)) {
            // +50% phí thuê và làm tròn lên.
            long num = baseRent * 150L;
            return (num + 99L) / 100L;
        }
        return baseRent;
    }

    private void ensureNoTeamUpPending(Long gameId) {
        if (pendingTeamUpByGameId.containsKey(gameId)) {
            throw new RuntimeException("Đang chờ quyết định team-up");
        }
    }

    private List<GameStateResponse.FinalRankingEntryDto> buildFinalRanking(Game game) {
        List<GamePlayer> list = gamePlayerRepository.findByGameIdOrderByTurnOrderAsc(game.getGameId());
        List<GamePlayer> ranked = new ArrayList<>(list);
        Long winnerTeamOwnerId = game.getWinnerPlayerId();

        // Xếp hạng team dựa trên eliminationOrder của đội trưởng (team-owner).
        Map<Long, Integer> teamEliminationOrderByOwner = new HashMap<>();
        for (GamePlayer gp : ranked) {
            Long teamOwnerId = resolveTeamOwnerGamePlayerId(gp);
            if (teamOwnerId != null && Objects.equals(teamOwnerId, gp.getGamePlayerId())) {
                teamEliminationOrderByOwner.put(teamOwnerId, gp.getEliminationOrder());
            }
        }

        ranked.sort(
                (a, b) -> {
                    Long aTeamOwnerId = resolveTeamOwnerGamePlayerId(a);
                    Long bTeamOwnerId = resolveTeamOwnerGamePlayerId(b);

                    // Cùng team: phụ thuộc luôn xếp trên chủ team 1 bậc.
                    if (aTeamOwnerId != null && Objects.equals(aTeamOwnerId, bTeamOwnerId)) {
                        boolean aDep = isTeamDependent(a);
                        boolean bDep = isTeamDependent(b);
                        if (aDep != bDep) {
                            return aDep ? -1 : 1;
                        }
                        int at = a.getTurnOrder() != null ? a.getTurnOrder() : 0;
                        int bt = b.getTurnOrder() != null ? b.getTurnOrder() : 0;
                        return Integer.compare(at, bt);
                    }

                    boolean aWinTeam = winnerTeamOwnerId != null && Objects.equals(aTeamOwnerId, winnerTeamOwnerId);
                    boolean bWinTeam = winnerTeamOwnerId != null && Objects.equals(bTeamOwnerId, winnerTeamOwnerId);
                    if (aWinTeam && !bWinTeam) return -1;
                    if (!aWinTeam && bWinTeam) return 1;

                    Integer ea = aTeamOwnerId != null ? teamEliminationOrderByOwner.get(aTeamOwnerId) : null;
                    Integer eb = bTeamOwnerId != null ? teamEliminationOrderByOwner.get(bTeamOwnerId) : null;

                    if (ea == null && eb != null) return -1;
                    if (ea != null && eb == null) return 1;
                    if (ea == null && eb == null) {
                        int at = a.getTurnOrder() != null ? a.getTurnOrder() : 0;
                        int bt = b.getTurnOrder() != null ? b.getTurnOrder() : 0;
                        return Integer.compare(at, bt);
                    }
                    // eliminationOrder lớn hơn = bị loại muộn hơn = xếp hạng tốt hơn
                    return Integer.compare(eb, ea);
                });
        List<GameStateResponse.FinalRankingEntryDto> out = new ArrayList<>();
        int r = 1;
        for (GamePlayer gp : ranked) {
            String heroName = null;
            if (gp.getCharacterId() != null) {
                heroName =
                        heroRepository.findById(gp.getCharacterId()).map(Hero::getName).orElse(null);
            }
            Long coinReward = rankingCoinRewardForDisplay(gp);
            out.add(
                    GameStateResponse.FinalRankingEntryDto.builder()
                            .rank(r++)
                            .turnOrder(gp.getTurnOrder())
                            .displayName(displayNameForPlayer(gp))
                            .heroName(heroName)
                            .balance(gp.getBalance() == null ? 0L : gp.getBalance())
                            .coinReward(coinReward)
                            .isBot(gp.getIsBot())
                            .build());
        }
        return out;
    }

    /** Bot: không thưởng xu; người: xu đã cấp (null nếu dữ liệu ván cũ). */
    private Long rankingCoinRewardForDisplay(GamePlayer gp) {
        if (Boolean.TRUE.equals(gp.getIsBot())) {
            return null;
        }
        if (gp.getEndMatchCoinReward() != null) {
            return gp.getEndMatchCoinReward().longValue();
        }
        return null;
    }

    /**
     * Mỗi người chơi thật nhận ngẫu nhiên 10–200 xu vào tài khoản; chỉ chạy một lần khi ván kết thúc.
     */
    private void awardEndMatchCoins(Long gameId) {
        Game g = gameRepository.findById(gameId).orElse(null);
        if (g != null && Boolean.TRUE.equals(g.getSoloVsAi())) {
            return;
        }
        List<GamePlayer> players = gamePlayerRepository.findByGameIdOrderByTurnOrderAsc(gameId);
        for (GamePlayer gp : players) {
            if (Boolean.TRUE.equals(gp.getIsBot()) || gp.getUserProfileId() == null) {
                continue;
            }
            if (gp.getEndMatchCoinReward() != null) {
                continue;
            }
            int reward = 10 + random.nextInt(191);
            gp.setEndMatchCoinReward(reward);
            userProfileRepository
                    .findById(gp.getUserProfileId())
                    .ifPresent(
                            profile -> {
                                long curGold = profile.getGold() == null ? 0L : profile.getGold();
                                profile.setGold(curGold + reward);
                                userProfileRepository.save(profile);
                            });
            gamePlayerRepository.save(gp);
        }
    }

    /** Gán thứ tự bị loại  */
    private void assignEliminationOrderOnBankruptcy(Game game, GamePlayer p) {
        if (p.getEliminationOrder() != null) {
            return;
        }
        Game g = getGame(game.getGameId());
        int seq = g.getEliminationSequence() == null ? 0 : g.getEliminationSequence();
        seq++;
        g.setEliminationSequence(seq);
        p.setEliminationOrder(seq);
        gameRepository.save(g);
        gamePlayerRepository.save(p);
    }

    private List<GameStateResponse.OwnedCellDto> buildOwnedCellsSnapshot(Game game) {
        Long gameId = game.getGameId();
        List<BoardCell> ordered = listBoardCellsInPlayOrder(game);
        List<PlayerProperty> props = playerPropertyRepository.findByGame_GameId(gameId);
        Map<Integer, PlayerProperty> byCellId = new HashMap<>(Math.max(16, props.size() * 2));
        for (PlayerProperty pp : props) {
            if (pp.getBoardCell() != null) {
                byCellId.put(pp.getBoardCell().getCellId(), pp);
            }
        }
        List<GameStateResponse.OwnedCellDto> out = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            BoardCell bc = ordered.get(i);
            PlayerProperty pp = byCellId.get(bc.getCellId());
            if (pp != null
                    && pp.getOwnerPlayer() != null
                    && pp.getOwnerPlayer().getTurnOrder() != null) {
                out.add(
                        GameStateResponse.OwnedCellDto.builder()
                                .boardIndex(i)
                                .ownerTurnOrder(pp.getOwnerPlayer().getTurnOrder())
                                .build());
            }
        }
        return out;
    }

    @Transactional
    public GameActionResponse rollDice(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);

        ensureNoTeamUpPending(gameId);

        if ("INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Bạn đang nợ tiền thuê — hãy bán tài sản hoặc phá sản");
        }
        if (!"WAIT_ROLL".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Lượt hiện tại không thể tung xúc xắc");
        }

        RollDiceOutcome rolled = performRollAndMove(game, player);
        String message = "Bạn tung được " + rolled.d1() + " + " + rolled.d2();
        if (rolled.passGoBonus() > 0) {
            message += " · Qua ô xuất phát +" + rolled.passGoBonus();
        }
        return actionResult(gameId, message, accountId);
    }

    /**
     * Thần xúc xắc — tung với hai mặt đã chọn (gọi từ {@link com.game.monopoly.service.skill.SkillActivationService}).
     */
    @Transactional
    public String applyChosenDiceRollForSkill(Long gameId, Long accountId, int d1, int d2) {
        Game game = getGame(gameId);
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);
        if ("INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Bạn đang nợ tiền thuê — hãy bán tài sản hoặc phá sản");
        }
        if (!"WAIT_ROLL".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Lượt hiện tại không thể tung xúc xắc");
        }
        player = gamePlayerRepository.findById(player.getGamePlayerId()).orElseThrow();
        RollDiceOutcome rolled =
                Boolean.TRUE.equals(player.getInJail())
                        ? performJailRollWithDice(game, player, d1, d2)
                        : performNormalRollWithDice(game, player, d1, d2);
        return rolled.d1() + " + " + rolled.d2();
    }

    @Transactional
    public GameActionResponse activateSkill(Long gameId, Long accountId, SkillActivateRequest request) {
        ensureNoTeamUpPending(gameId);
        String msg =
                skillActivationService.performActivate(
                        gameId, accountId, request, line -> enqueueGameLog(gameId, line));
        return actionResult(gameId, msg, accountId);
    }

    /**
     * Mua ô hiện tại theo {@link GamePlayer#getPosition()} — trong cả pha {@code ACTION_REQUIRED}
     * (cả lượt sau khi đi, không bắt buộc thao tác ngay lúc vừa dừng quân).
     */
    @Transactional
    public GameActionResponse buyCurrentCell(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);

        ensureNoTeamUpPending(gameId);

        if ("INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Bạn đang nợ tiền thuê — không thể mua ô");
        }
        if (!"ACTION_REQUIRED".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Không thể mua ô ở thời điểm hiện tại");
        }
        if (pendingOpponentLandByGameId.containsKey(game.getGameId())) {
            throw new RuntimeException("Hãy chọn trả thuê hoặc mua lại đất đối thủ trước");
        }

        BoardCell cell = getCellByPosition(game, player.getPosition());
        if (!isPurchasableCell(cell)) {
            throw new RuntimeException("Ô hiện tại không thể mua");
        }

        Optional<PlayerProperty> existing = playerPropertyRepository.findByGame_GameIdAndBoardCell_CellId(gameId, cell.getCellId());
        if (existing.isPresent() && existing.get().getOwnerPlayer() != null) {
            throw new RuntimeException("Ô này đã có chủ");
        }

        long price = getCellPrice(cell);
        long teamBalance = getTeamBalance(player);
        if (teamBalance < price) {
            throw new RuntimeException("Không đủ tiền để mua ô");
        }

        setTeamBalance(player, teamBalance - price);

        PlayerProperty property = existing.orElseGet(() -> {
            PlayerProperty pp = new PlayerProperty();
            pp.setId(new PlayerPropertyId(gameId, cell.getCellId()));
            pp.setGame(game);
            pp.setBoardCell(cell);
            pp.setHouseLevel(0);
            return pp;
        });
        GamePlayer teamOwner = resolveTeamOwner(player);
        property.setOwnerPlayer(teamOwner);
        property.setHouseLevel(0);
        property.setUpgradeSpentTotal(0L);
        playerPropertyRepository.save(property);

        game.setHumanTurnStartedAt(LocalDateTime.now());
        gameRepository.save(game);

        String cellLabel = cell.getName() != null ? cell.getName() : "Ô";
        logSpendOnly(
                gameId,
                displayNameForPlayer(player)
                        + " tiêu tiền tại 「"
                        + cellLabel
                        + "」 để mua ô trống: "
                        + formatMoneyLog(price)
                        + ".",
                player,
                price);

        return actionResult(gameId, "Mua ô " + cell.getName() + " thành công", accountId);
    }

    @Transactional
    public GameActionResponse resolveOpponentLand(Long gameId, Long accountId, boolean buyback) {
        Game game = getGame(gameId);
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);

        ensureNoTeamUpPending(gameId);

        if ("INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Bạn đang nợ tiền thuê — không thể thực hiện");
        }
        if (!"ACTION_REQUIRED".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Không thể thực hiện lúc này");
        }
        OpponentLandPending p = pendingOpponentLandByGameId.get(gameId);
        if (p == null) {
            throw new RuntimeException("Không có lựa chọn mua lại / trả thuê");
        }
        BoardCell at = getCellByPosition(game, player.getPosition());
        if (at.getCellId() == null || !Objects.equals(at.getCellId(), p.cellId)) {
            pendingOpponentLandByGameId.remove(gameId);
            throw new RuntimeException("Ô không khớp — hãy tải lại trạng thái");
        }
        BoardCell cell = boardCellRepository.findById(p.cellId).orElseThrow();
        PlayerProperty prop =
                playerPropertyRepository.findByGame_GameIdAndBoardCell_CellId(gameId, p.cellId).orElseThrow();
        GamePlayer owner = prop.getOwnerPlayer();
        if (owner == null || !Objects.equals(owner.getGamePlayerId(), p.ownerGamePlayerId)) {
            pendingOpponentLandByGameId.remove(gameId);
            throw new RuntimeException("Ô đã đổi chủ");
        }
        if (Objects.equals(owner.getGamePlayerId(), player.getGamePlayerId())) {
            pendingOpponentLandByGameId.remove(gameId);
            return actionResult(gameId, "Ô của bạn", accountId);
        }

        if (buyback) {
            long price = p.buybackPrice;
            long teamBal = getTeamBalance(player);
            if (teamBal < price) {
                throw new RuntimeException("Không đủ tiền để mua lại");
            }
            clearSkillBuybackMarkForCell(player, p.cellId);
            pendingOpponentLandByGameId.remove(gameId);
            setTeamBalance(player, teamBal - price);
            long ownerTeamBal = getTeamBalance(owner);
            setTeamBalance(owner, ownerTeamBal + price);

            GamePlayer buyerTeamOwner = resolveTeamOwner(player);
            prop.setOwnerPlayer(buyerTeamOwner);
            playerPropertyRepository.save(prop);
            game.setHumanTurnStartedAt(LocalDateTime.now());
            gameRepository.save(game);
            String cl = cell.getName() != null ? cell.getName() : "Ô";
            String recvNote =
                    p.buybackPercent != 130
                            ? "mua lại đất, ưu đãi kỹ năng Điều Khoản Vàng ("
                                    + p.buybackPercent
                                    + "% giá niêm yết)"
                            : "mua lại đất (" + p.buybackPercent + "% giá niêm yết)";
            logMoneyTransferThreeLines(
                    gameId,
                    displayNameForPlayer(player)
                            + " tiêu tiền tại 「"
                            + cl
                            + "」 để mua lại từ "
                            + displayNameForPlayer(owner)
                            + ": "
                            + formatMoneyLog(price)
                            + ".",
                    player,
                    owner,
                    price,
                    recvNote);
            return actionResult(gameId, "Đã mua lại ô " + cell.getName(), accountId);
        }

        clearSkillBuybackMarkForCell(player, p.cellId);
        pendingOpponentLandByGameId.remove(gameId);
        payRentAfterLanding(game, player, owner, cell, p.rentAmount);
        game = getGame(gameId);
        game.setHumanTurnStartedAt(LocalDateTime.now());
        gameRepository.save(game);
        return actionResult(gameId, "Đã trả tiền thuê", accountId);
    }

    @Transactional
    public GameActionResponse teamUpInvite(Long gameId, Long accountId, boolean invite) {
        Game game = getGame(gameId);
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new RuntimeException("Ván đã kết thúc");
        }
        TeamUpPending p = pendingTeamUpByGameId.get(gameId);
        if (p == null || !TURN_STATE_TEAMUP_INVITE.equalsIgnoreCase(p.phase)) {
            throw new RuntimeException("Chưa có lời mời team-up cần xử lý");
        }
        if (accountId == null) {
            throw new RuntimeException("Thiếu X-Account-Id");
        }

        UserProfile profile = getProfileByAccountId(accountId);
        GamePlayer creditor =
                gamePlayerRepository.findById(p.creditorGamePlayerId).orElseThrow();
        if (!Objects.equals(creditor.getUserProfileId(), profile.getUserProfileId())) {
            throw new RuntimeException("Bạn không phải đội trưởng trong lời mời này");
        }

        if (!invite) {
            pendingTeamUpByGameId.remove(gameId);
            advanceTurn(game);
            return actionResult(gameId, "Bỏ qua team-up", accountId);
        }

        pendingTeamUpByGameId.put(
                gameId, new TeamUpPending(p.creditorGamePlayerId, p.dependentGamePlayerId, TURN_STATE_TEAMUP_ACCEPT));
        game.setTurnState(TURN_STATE_TEAMUP_ACCEPT);
        // Chuyển lượt sang phụ thuộc để người này ra quyết định.
        GamePlayer dependent =
                gamePlayerRepository.findById(p.dependentGamePlayerId).orElseThrow();
        game.setCurrentPlayerOrder(dependent.getTurnOrder());
        game.setHumanTurnStartedAt(LocalDateTime.now());
        gameRepository.save(game);

        enqueueGameLog(gameId, displayNameForPlayer(creditor) + " đã mời " + p.dependentGamePlayerId + " vào team-up.");
        return actionResult(gameId, "Đã gửi lời mời team-up", accountId);
    }

    @Transactional
    public GameActionResponse teamUpRespond(Long gameId, Long accountId, boolean accept) {
        Game game = getGame(gameId);
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new RuntimeException("Ván đã kết thúc");
        }
        TeamUpPending p = pendingTeamUpByGameId.get(gameId);
        if (p == null || !TURN_STATE_TEAMUP_ACCEPT.equalsIgnoreCase(p.phase)) {
            throw new RuntimeException("Chưa có phản hồi team-up cần xử lý");
        }
        if (accountId == null) {
            throw new RuntimeException("Thiếu X-Account-Id");
        }

        UserProfile profile = getProfileByAccountId(accountId);
        GamePlayer dependent =
                gamePlayerRepository.findById(p.dependentGamePlayerId).orElseThrow();
        if (!Objects.equals(dependent.getUserProfileId(), profile.getUserProfileId())) {
            throw new RuntimeException("Bạn không phải phụ thuộc trong lời mời này");
        }

        if (!accept) {
            pendingTeamUpByGameId.remove(gameId);
            advanceTurn(game);
            return actionResult(gameId, "Từ chối team-up", accountId);
        }

        GamePlayer creditor = gamePlayerRepository.findById(p.creditorGamePlayerId).orElseThrow();
        GamePlayer creditorTeamOwner = resolveTeamOwner(creditor);
        long sharedBalance = getTeamBalance(creditorTeamOwner);

        // Revive phụ thuộc
        dependent.setIsBankrupt(false);
        dependent.setTeamOwnerGamePlayerId(creditorTeamOwner.getGamePlayerId());
        dependent.setTeamPartnerGamePlayerId(creditorTeamOwner.getGamePlayerId());

        // Thiết lập cặp
        creditorTeamOwner.setTeamOwnerGamePlayerId(creditorTeamOwner.getGamePlayerId());
        creditorTeamOwner.setTeamPartnerGamePlayerId(dependent.getGamePlayerId());

        // Đồng bộ balance dùng chung
        setTeamBalance(dependent, sharedBalance);

        gamePlayerRepository.save(creditorTeamOwner);
        gamePlayerRepository.save(dependent);

        pendingTeamUpByGameId.remove(gameId);
        enqueueGameLog(gameId, displayNameForPlayer(creditorTeamOwner) + " cứu " + displayNameForPlayer(dependent) + " thành phụ thuộc (phí thuê +50%).");

        // Sau khi được cứu, chèn ngay 1 lượt của phụ thuộc để thực hiện buy/upgrade trước khi lắc xúc xắc tiếp.
        game = getGame(gameId);
        game.setTurnState("ACTION_REQUIRED");
        game.setCurrentPlayerOrder(dependent.getTurnOrder());
        game.setHumanTurnStartedAt(LocalDateTime.now());
        gameRepository.save(game);

        return actionResult(gameId, "Đã team-up thành công", accountId);
    }

    /** Bot luôn trả thuê (không mua lại) khi có pending. */
    private void resolveOpponentLandForBot(Game game, GamePlayer bot) {
        Long gid = game.getGameId();
        OpponentLandPending p = pendingOpponentLandByGameId.get(gid);
        if (p == null) {
            return;
        }
        BoardCell at = getCellByPosition(game, bot.getPosition());
        if (at.getCellId() == null || !Objects.equals(at.getCellId(), p.cellId)) {
            return;
        }
        PlayerProperty prop =
                playerPropertyRepository.findByGame_GameIdAndBoardCell_CellId(gid, p.cellId).orElse(null);
        if (prop == null || prop.getOwnerPlayer() == null) {
            pendingOpponentLandByGameId.remove(gid);
            return;
        }
        GamePlayer owner = prop.getOwnerPlayer();
        if (!Objects.equals(owner.getGamePlayerId(), p.ownerGamePlayerId)) {
            pendingOpponentLandByGameId.remove(gid);
            return;
        }
        BoardCell cell = boardCellRepository.findById(p.cellId).orElse(null);
        if (cell == null) {
            pendingOpponentLandByGameId.remove(gid);
            return;
        }
        clearSkillBuybackMarkForCell(bot, p.cellId);
        pendingOpponentLandByGameId.remove(gid);
        payRentAfterLanding(game, bot, owner, cell, p.rentAmount);
        game = getGame(gid);
        gameRepository.save(game);
    }

    @Transactional
    public GameActionResponse upgradeCurrentCell(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);

        ensureNoTeamUpPending(gameId);

        if ("INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Bạn đang nợ tiền thuê — không thể nâng cấp");
        }
        if (!"ACTION_REQUIRED".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Không thể nâng cấp ở thời điểm hiện tại");
        }
        if (pendingOpponentLandByGameId.containsKey(game.getGameId())) {
            throw new RuntimeException("Hãy chọn trả thuê hoặc mua lại đất đối thủ trước");
        }

        BoardCell cell = getCellByPosition(game, player.getPosition());
        PlayerProperty property = playerPropertyRepository.findByGame_GameIdAndBoardCell_CellId(gameId, cell.getCellId())
                .orElseThrow(() -> new RuntimeException("Bạn chưa sở hữu ô này"));

        Long effectiveTeamOwnerId = resolveTeamOwnerGamePlayerId(player);
        if (property.getOwnerPlayer() == null
                || !Objects.equals(property.getOwnerPlayer().getGamePlayerId(), effectiveTeamOwnerId)) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có thể nâng cấp");
        }

        int maxLevel = cell.getMaxHouseLevel() == null ? 5 : cell.getMaxHouseLevel();
        int currentLevel = property.getHouseLevel() == null ? 0 : property.getHouseLevel();
        if (currentLevel >= maxLevel) {
            throw new RuntimeException("Ô đã đạt cấp tối đa");
        }

        long upgradeCost = upgradeCostForCell(cell);
        long teamBalance = getTeamBalance(player);
        if (teamBalance < upgradeCost) {
            throw new RuntimeException("Không đủ tiền để nâng cấp");
        }

        setTeamBalance(player, teamBalance - upgradeCost);
        property.setHouseLevel(currentLevel + 1);
        long spent = property.getUpgradeSpentTotal() == null ? 0L : property.getUpgradeSpentTotal();
        property.setUpgradeSpentTotal(spent + upgradeCost);
        playerPropertyRepository.save(property);

        game.setHumanTurnStartedAt(LocalDateTime.now());
        gameRepository.save(game);

        String cn = cell.getName() != null ? cell.getName() : "Ô";
        logSpendOnly(
                gameId,
                displayNameForPlayer(player)
                        + " tiêu tiền tại 「"
                        + cn
                        + "」 để nâng cấp lên cấp "
                        + property.getHouseLevel()
                        + ": "
                        + formatMoneyLog(upgradeCost)
                        + ".",
                player,
                upgradeCost);

        return actionResult(gameId, "Nâng cấp ô " + cell.getName() + " lên cấp " + property.getHouseLevel(), accountId);
    }

    @Transactional
    public GameActionResponse endTurn(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);

        ensureNoTeamUpPending(gameId);

        if ("INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Bạn đang nợ tiền thuê — hãy bán tài sản hoặc phá sản trước");
        }
        if ("WAIT_ROLL".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Hãy tung xúc xắc trước khi kết thúc lượt");
        }
        if (pendingOpponentLandByGameId.containsKey(game.getGameId())) {
            throw new RuntimeException("Hãy chọn trả thuê hoặc mua lại đất đối thủ (130%) trước khi kết thúc lượt");
        }

        player.setConsecutiveDoubles(0);
        gamePlayerRepository.save(player);
        enqueueGameLog(gameId, displayNameForPlayer(player) + " kết thúc lượt.");
        advanceTurn(game);
        return actionResult(gameId, "Kết thúc lượt", accountId);
    }

    private record RollDiceOutcome(int d1, int d2, long passGoBonus) {
        Integer[] dicePair() {
            return new Integer[]{d1, d2};
        }
    }

    private RollDiceOutcome performRollAndMove(Game game, GamePlayer player) {
        player = gamePlayerRepository.findById(player.getGamePlayerId()).orElseThrow();
        if (Boolean.TRUE.equals(player.getInJail())) {
            return performJailRoll(game, player);
        }
        return performNormalRoll(game, player);
    }

    private RollDiceOutcome performJailRoll(Game game, GamePlayer player) {
        int d1 = random.nextInt(6) + 1;
        int d2 = random.nextInt(6) + 1;
        return performJailRollWithDice(game, player, d1, d2);
    }

    private RollDiceOutcome performJailRollWithDice(Game game, GamePlayer player, int d1, int d2) {
        lastDiceByGame.put(game.getGameId(), new Integer[]{d1, d2});
        int fails = player.getJailFailedRolls() == null ? 0 : player.getJailFailedRolls();

        Integer orderSnap = game.getCurrentPlayerOrder();
        if (fails >= 3) {
            player.setInJail(false);
            player.setJailFailedRolls(0);
            player.setConsecutiveDoubles(0);
            gamePlayerRepository.save(player);
            long passGo = applyDiceMovementAndLanding(game, player, d1, d2);
            player = gamePlayerRepository.findById(player.getGamePlayerId()).orElseThrow();
            return finalizeAfterRoll(game, player, d1, d2, passGo, false, orderSnap);
        }
        if (d1 == d2) {
            player.setInJail(false);
            player.setJailFailedRolls(0);
            player.setConsecutiveDoubles(0);
            gamePlayerRepository.save(player);
            long passGo = applyDiceMovementAndLanding(game, player, d1, d2);
            player = gamePlayerRepository.findById(player.getGamePlayerId()).orElseThrow();
            return finalizeAfterRoll(game, player, d1, d2, passGo, false, orderSnap);
        }
        enqueueGameLog(
                game.getGameId(),
                displayNameForPlayer(player)
                        + " ở tù, lắc "
                        + d1
                        + " + "
                        + d2
                        + " (không đôi) — mất lượt ("
                        + (fails + 1)
                        + "/3).");
        player.setJailFailedRolls(fails + 1);
        gamePlayerRepository.save(player);
        advanceTurn(game);
        return new RollDiceOutcome(d1, d2, 0L);
    }

    private RollDiceOutcome performNormalRoll(Game game, GamePlayer player) {
        int d1 = random.nextInt(6) + 1;
        int d2 = random.nextInt(6) + 1;
        return performNormalRollWithDice(game, player, d1, d2);
    }

    private RollDiceOutcome performNormalRollWithDice(Game game, GamePlayer player, int d1, int d2) {
        Integer orderSnap = game.getCurrentPlayerOrder();
        lastDiceByGame.put(game.getGameId(), new Integer[]{d1, d2});
        boolean isDouble = d1 == d2;
        if (isDouble) {
            int cd = player.getConsecutiveDoubles() == null ? 0 : player.getConsecutiveDoubles();
            cd++;
            if (cd >= 3) {
                enqueueGameLog(
                        game.getGameId(),
                        displayNameForPlayer(player) + " lắc đôi lần thứ 3 — vào tù.");
                player.setConsecutiveDoubles(0);
                gamePlayerRepository.save(player);
                sendToJail(game, player);
                advanceTurn(game);
                return new RollDiceOutcome(d1, d2, 0L);
            }
            player.setConsecutiveDoubles(cd);
        } else {
            player.setConsecutiveDoubles(0);
        }
        gamePlayerRepository.save(player);
        long passGo = applyDiceMovementAndLanding(game, player, d1, d2);
        player = gamePlayerRepository.findById(player.getGamePlayerId()).orElseThrow();
        boolean allowExtra = d1 == d2;
        return finalizeAfterRoll(game, player, d1, d2, passGo, allowExtra, orderSnap);
    }

    private long applyDiceMovementAndLanding(Game game, GamePlayer player, int d1, int d2) {
        int boardCells = getBoardCellCount(game);
        int oldPos = player.getPosition() == null ? 0 : player.getPosition();
        int delta = d1 + d2;
        int raw = oldPos + delta;
        int nextPos = boardCells == 0 ? 0 : raw % boardCells;
        long balance = getTeamBalance(player);
        long laps = boardCells <= 0 ? 0L : raw / boardCells;
        long passGoBonus = laps * MonopolyGameRules.PASS_GO_BONUS;
        logDiceMovement(game, player, d1, d2, passGoBonus);
        setTeamBalance(player, balance + passGoBonus);
        player.setPosition(nextPos);
        gamePlayerRepository.save(player);
        applyLandingEffect(game, player);
        return passGoBonus;
    }

    private RollDiceOutcome finalizeAfterRoll(
            Game game,
            GamePlayer player,
            int d1,
            int d2,
            long passGoBonus,
            boolean allowExtraRollIfDouble,
            Integer orderBeforeMove) {
        game = getGame(game.getGameId());

        if ("INSOLVENT".equalsIgnoreCase(game.getTurnState())
                && Boolean.TRUE.equals(player.getIsBot())) {
            advanceOneBotDebtStep(game, player);
            game = getGame(game.getGameId());
        }

        if ("INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            game.setHumanTurnStartedAt(LocalDateTime.now());
            gameRepository.save(game);
            return new RollDiceOutcome(d1, d2, passGoBonus);
        }

        if (!Objects.equals(orderBeforeMove, game.getCurrentPlayerOrder())) {
            return new RollDiceOutcome(d1, d2, passGoBonus);
        }

        boolean isDouble = d1 == d2;
        if (allowExtraRollIfDouble && isDouble) {
            pendingChanceExtraRollByGameId.remove(game.getGameId());
            game.setTurnState("WAIT_ROLL");
            if (!Boolean.TRUE.equals(player.getIsBot())) {
                game.setHumanTurnStartedAt(LocalDateTime.now());
            } else {
                game.setHumanTurnStartedAt(null);
            }
            gameRepository.save(game);
            return new RollDiceOutcome(d1, d2, passGoBonus);
        }

        if (Boolean.TRUE.equals(pendingChanceExtraRollByGameId.remove(game.getGameId()))) {
            game.setTurnState("WAIT_ROLL");
            if (!Boolean.TRUE.equals(player.getIsBot())) {
                game.setHumanTurnStartedAt(LocalDateTime.now());
            } else {
                game.setHumanTurnStartedAt(null);
            }
            gameRepository.save(game);
            return new RollDiceOutcome(d1, d2, passGoBonus);
        }

        game.setTurnState("ACTION_REQUIRED");
        if (!Boolean.TRUE.equals(player.getIsBot())) {
            game.setHumanTurnStartedAt(LocalDateTime.now());
        } else {
            game.setHumanTurnStartedAt(null);
        }
        gameRepository.save(game);
        return new RollDiceOutcome(d1, d2, passGoBonus);
    }

    private int getJailBoardIndex(Game game) {
        List<BoardCell> order = listBoardCellsInPlayOrder(game);
        for (int i = 0; i < order.size(); i++) {
            String t = order.get(i).getType();
            if (t != null) {
                String u = t.toUpperCase(Locale.ROOT);
                if (u.contains("JAIL_VISIT")) {
                    return i;
                }
            }
        }
        for (int i = 0; i < order.size(); i++) {
            String n = order.get(i).getName();
            if (n != null && "jail".equalsIgnoreCase(n.trim())) {
                return i;
            }
        }
        return Math.min(10, Math.max(0, order.size() - 1));
    }

    private boolean isGoToJailCell(BoardCell cell) {
        if (cell.getType() == null) {
            return false;
        }
        String u = cell.getType().toUpperCase(Locale.ROOT);
        return u.contains("GOTO_JAIL") || u.contains("GO_TO_JAIL");
    }

    private void sendToJail(Game game, GamePlayer p) {
        int idx = getJailBoardIndex(game);
        p.setPosition(idx);
        p.setInJail(true);
        p.setJailFailedRolls(0);
        p.setConsecutiveDoubles(0);
        gamePlayerRepository.save(p);
    }

    private boolean isCommunityChestCell(BoardCell cell) {
        if (cell.getType() == null) {
            return false;
        }
        String u = cell.getType().toUpperCase(Locale.ROOT);
        return u.contains("COMMUNITY");
    }

    /** Community Chest: tiền ngẫu nhiên 50–300. */
    private void grantChestRandomMoney(Game game, GamePlayer p, String labelSuffix) {
        long gift = 50L + random.nextInt(251);
        long bal = getTeamBalance(p);
        setTeamBalance(p, bal + gift);
        logReceiveOnly(
                game.getGameId(),
                displayNameForPlayer(p) + " nhận tiền 「Community Chest」.",
                p,
                gift,
                labelSuffix);
    }

    private boolean isTaxCell(BoardCell cell) {
        if (cell.getType() == null) {
            return false;
        }
        return cell.getType().toUpperCase(Locale.ROOT).contains("TAX");
    }

    /** Thu nhập ~10%, thuế xa xỉ ~30% trên số dư hiện có. */
    private void applyTaxLanding(Game game, GamePlayer player, BoardCell cell) {
        long bal = getTeamBalance(player);
        if (bal <= 0L) {
            enqueueGameLog(
                    game.getGameId(),
                    displayNameForPlayer(player)
                            + " vào ô thuế «"
                            + (cell.getName() != null ? cell.getName() : "Thuế")
                            + "» — số dư 0, không thu.");
            return;
        }
        String n = cell.getName() != null ? cell.getName().toLowerCase(Locale.ROOT) : "";
        int pct = (n.contains("luxury") || n.contains("xa xỉ") || n.contains("xa xi")) ? 30 : 10;
        long due = bal * pct / 100L;
        due = Math.min(due, bal);
        if (due <= 0L) {
            return;
        }
        setTeamBalance(player, bal - due);
        String taxCell = cell.getName() != null ? cell.getName() : "Thuế";
        logSpendOnly(
                game.getGameId(),
                displayNameForPlayer(player)
                        + " tiêu tiền tại 「"
                        + taxCell
                        + "」 cho thuế ("
                        + pct
                        + "% số dư: "
                        + formatMoneyLog(due)
                        + ").",
                player,
                due);
    }

    private void clearSkillBuybackMarkForCell(GamePlayer player, int cellId) {
        GamePlayer fresh = gamePlayerRepository.findById(player.getGamePlayerId()).orElse(player);
        if (fresh.getSkillMarkedCellId() != null && fresh.getSkillMarkedCellId().equals(cellId)) {
            fresh.setSkillMarkedCellId(null);
            fresh.setSkillBuybackPercent(null);
            gamePlayerRepository.save(fresh);
        }
    }

    private void payRentAfterLanding(Game game, GamePlayer currentPlayer, GamePlayer owner, BoardCell cell, long rent) {
        long actualRent = applyDependentRentPenaltyIfNeeded(currentPlayer, rent);
        long payerBalance = getTeamBalance(currentPlayer);
        long ownerBalance = getTeamBalance(owner);
        String cellName = cell.getName() != null && !cell.getName().isBlank() ? cell.getName() : "Ô";

        // Cùng team: dùng chung tài nguyên nên tiền thuê không tạo ảnh hưởng.
        if (Objects.equals(resolveTeamOwnerGamePlayerId(currentPlayer), resolveTeamOwnerGamePlayerId(owner))) {
            return;
        }

        if (actualRent <= payerBalance) {
            setTeamBalance(currentPlayer, payerBalance - actualRent);
            setTeamBalance(owner, ownerBalance + actualRent);
            String action =
                    displayNameForPlayer(currentPlayer)
                            + " tiêu tiền tại 「"
                            + cellName
                            + "」 cho tiền thuê ("
                            + displayNameForPlayer(owner)
                            + "): "
                            + formatMoneyLog(actualRent)
                            + ".";
            logMoneyTransferThreeLines(game.getGameId(), action, currentPlayer, owner, actualRent, null);
            enqueueRentNotice(game, currentPlayer, owner, cell, actualRent);
            return;
        }

        Long assetOwnerId = resolveTeamOwnerGamePlayerId(currentPlayer);
        List<PlayerProperty> myAssets =
                playerPropertyRepository.findByGame_GameIdAndOwnerPlayer_GamePlayerId(
                        game.getGameId(), assetOwnerId);
        if (!canLiquidateAny(myAssets)) {
            long paidAll = payerBalance;
            String action =
                    displayNameForPlayer(currentPlayer)
                            + " không đủ tiền trả tiền thuê tại 「"
                            + cellName
                            + "」 — chuyển toàn bộ tiền mặt "
                            + formatMoneyLog(paidAll)
                            + " cho "
                            + displayNameForPlayer(owner)
                            + " (phá sản).";
            logMoneyTransferThreeLines(
                    game.getGameId(),
                    action,
                    currentPlayer,
                    owner,
                    paidAll,
                    "tiền mặt còn lại khi phá sản nợ thuê");
            enqueueRentNotice(game, currentPlayer, owner, cell, paidAll);
            declareBankruptcyForDebtInternal(game, currentPlayer, owner);
            return;
        }

        game.setDebtRentAmount(actualRent);
        game.setDebtCreditorGamePlayerId(owner.getGamePlayerId());
        game.setDebtCellId(cell.getCellId());
        game.setTurnState("INSOLVENT");
        gameRepository.save(game);
    }

    private boolean isChanceCell(BoardCell cell) {
        if (cell.getType() == null) {
            return false;
        }
        return cell.getType().toUpperCase(Locale.ROOT).contains("CHANCE");
    }

    private boolean isJailVisitCell(BoardCell cell) {
        if (cell.getType() == null) {
            return false;
        }
        return cell.getType().toUpperCase(Locale.ROOT).contains("JAIL_VISIT");
    }

    private void applyLandingEffect(Game game, GamePlayer currentPlayer) {
        BoardCell cell = getCellByPosition(game, currentPlayer.getPosition());
        if (isGoToJailCell(cell)) {
            enqueueGameLog(
                    game.getGameId(),
                    displayNameForPlayer(currentPlayer) + " vào tù (ô Đi tù).");
            sendToJail(game, currentPlayer);
            advanceTurn(game);
            return;
        }
        if (isJailVisitCell(cell)) {
            enqueueGameLog(
                    game.getGameId(),
                    displayNameForPlayer(currentPlayer) + " vào ô Thăm ngục — bị khóa trong tù.");
            sendToJail(game, currentPlayer);
            advanceTurn(game);
            return;
        }
        if (isChanceCell(cell)) {
            pendingChanceExtraRollByGameId.put(game.getGameId(), Boolean.TRUE);
            enqueueGameLog(
                    game.getGameId(),
                    displayNameForPlayer(currentPlayer) + " vào ô Chance — được thêm một lượt tung xúc xắc.");
            return;
        }
        if (isTaxCell(cell)) {
            applyTaxLanding(game, currentPlayer, cell);
            return;
        }
        if (isCommunityChestCell(cell)) {
            grantChestRandomMoney(game, currentPlayer, "Community Chest");
            return;
        }
        Optional<PlayerProperty> ppOpt =
                playerPropertyRepository.findByGameIdAndCellIdWithOwner(game.getGameId(), cell.getCellId());
        if (ppOpt.isEmpty()) {
            return;
        }

        PlayerProperty property = ppOpt.get();
        GamePlayer owner = property.getOwnerPlayer();
        if (owner == null) {
            return;
        }
        // Nếu cùng team (share pool/tài sản) thì không tính tiền thuê.
        if (Objects.equals(resolveTeamOwnerGamePlayerId(owner), resolveTeamOwnerGamePlayerId(currentPlayer))) {
            return;
        }
        if (Boolean.TRUE.equals(owner.getIsBankrupt())) {
            return;
        }

        long rent = calculateRent(cell, property);
        long payerBalance = getTeamBalance(currentPlayer);
        long listPrice = getCellPrice(cell);
        GamePlayer curFresh =
                gamePlayerRepository.findById(currentPlayer.getGamePlayerId()).orElse(currentPlayer);
        int buybackPct = 130;
        if (curFresh.getSkillMarkedCellId() != null
                && cell.getCellId() != null
                && curFresh.getSkillMarkedCellId().equals(cell.getCellId())
                && curFresh.getSkillBuybackPercent() != null) {
            buybackPct = Math.max(1, Math.min(500, curFresh.getSkillBuybackPercent()));
        }
        // Khi mua lại ô của đối thủ, cần tính cả phần đã dùng để nâng cấp (nhà/cấp độ).
        // "Giá nhà" ở đây được định nghĩa là: giá ô gốc + tổng tiền đã nâng cấp.
        long buybackPrice;
        if (listPrice <= 0) {
            buybackPrice = Long.MAX_VALUE;
        } else {
            long buybackBaseValue = getHouseValue(cell, property);
            buybackPrice = (buybackBaseValue * buybackPct + 99L) / 100L;
        }

        if (isPurchasableCell(cell) && listPrice > 0 && payerBalance >= buybackPrice) {
            pendingOpponentLandByGameId.put(
                    game.getGameId(),
                    new OpponentLandPending(
                            rent, buybackPrice, cell.getCellId(), owner.getGamePlayerId(), buybackPct));
            return;
        }

        payRentAfterLanding(game, currentPlayer, owner, cell, rent);
    }

    private void enqueueRentNotice(
            Game game, GamePlayer payer, GamePlayer owner, BoardCell cell, long paid) {
        if (paid <= 0) {
            return;
        }
        String cellLabel = cell.getName() != null && !cell.getName().isBlank() ? cell.getName() : "Ô";
        GameStateResponse.RentNoticeDto notice =
                GameStateResponse.RentNoticeDto.builder()
                        .payerName(displayNameForPlayer(payer))
                        .amountPaid(paid)
                        .cellName(cellLabel)
                        .ownerName(displayNameForPlayer(owner))
                        .build();
        pendingRentNotices.compute(
                game.getGameId(),
                (k, v) -> {
                    List<GameStateResponse.RentNoticeDto> list = v != null ? v : new ArrayList<>();
                    list.add(notice);
                    return list;
                });
    }

    private boolean canLiquidateAny(List<PlayerProperty> owned) {
        if (owned == null || owned.isEmpty()) {
            return false;
        }
        for (PlayerProperty pp : owned) {
            int lvl = pp.getHouseLevel() == null ? 0 : pp.getHouseLevel();
            if (lvl > 0) {
                return true;
            }
        }
        for (PlayerProperty pp : owned) {
            int lvl = pp.getHouseLevel() == null ? 0 : pp.getHouseLevel();
            if (lvl == 0 && pp.getBoardCell() != null) {
                return true;
            }
        }
        return false;
    }

    private void transferAllPropertiesFromTo(Long gameId, GamePlayer from, GamePlayer to) {
        List<PlayerProperty> list =
                playerPropertyRepository.findByGame_GameIdAndOwnerPlayer_GamePlayerId(
                        gameId, from.getGamePlayerId());
        for (PlayerProperty pp : list) {
            pp.setOwnerPlayer(to);
            playerPropertyRepository.save(pp);
        }
    }

    private void clearDebtFields(Game game) {
        game.setDebtRentAmount(null);
        game.setDebtCreditorGamePlayerId(null);
        game.setDebtCellId(null);
    }

    /**
     * Một bước xử lý nợ cho bot (bán 1 tài sản / trả đủ / phá sản). Client poll liên tục để chạy tiếp.
     */
    private void advanceOneBotDebtStep(Game game, GamePlayer bot) {
        game = getGame(game.getGameId());
        bot = gamePlayerRepository.findById(bot.getGamePlayerId()).orElseThrow();
        if (!"INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            return;
        }
        GamePlayer creditor =
                gamePlayerRepository.findById(game.getDebtCreditorGamePlayerId()).orElse(null);
        if (creditor == null) {
            return;
        }
        long owed = game.getDebtRentAmount() == null ? 0L : game.getDebtRentAmount();
        long bal = bot.getBalance() == null ? 0L : bot.getBalance();
        if (bal >= owed) {
            settleDebtPayment(game, bot, creditor);
            return;
        }
        List<PlayerProperty> assets =
                playerPropertyRepository.findByGame_GameIdAndOwnerPlayer_GamePlayerId(
                        game.getGameId(), bot.getGamePlayerId());
        if (!canLiquidateAny(assets)) {
            declareBankruptcyForDebtInternal(game, bot, creditor);
            return;
        }
        PlayerProperty pick = pickLiquidationTarget(assets);
        if (pick == null) {
            declareBankruptcyForDebtInternal(game, bot, creditor);
            return;
        }
        liquidateOneStep(game, bot, pick);
        bot = gamePlayerRepository.findById(bot.getGamePlayerId()).orElseThrow();
        game = getGame(game.getGameId());
        owed = game.getDebtRentAmount() == null ? 0L : game.getDebtRentAmount();
        if ((bot.getBalance() == null ? 0L : bot.getBalance()) >= owed) {
            creditor =
                    gamePlayerRepository
                            .findById(game.getDebtCreditorGamePlayerId())
                            .orElseThrow();
            settleDebtPayment(game, bot, creditor);
        }
    }

    /**
     * Một hành động bot mỗi lần gọi (lắc / mua / nâng cấp / hết lượt / một bước trả nợ).
     * Không Thread.sleep — client poll nhanh khi không tới lượt người.
     */
    private void advanceOneBotStep(Game game) {
        Long gid = game.getGameId();
        game = getGame(gid);
        if (game.getStatus() != GameStatus.PLAYING) {
            return;
        }
        GamePlayer cur = getCurrentTurnPlayer(game);
        if (!Boolean.TRUE.equals(cur.getIsBot())) {
            return;
        }
        String ts = game.getTurnState();
        if ("INSOLVENT".equalsIgnoreCase(ts)) {
            advanceOneBotDebtStep(game, cur);
            return;
        }
        if ("WAIT_ROLL".equalsIgnoreCase(ts)) {
            performRollAndMove(game, cur);
            return;
        }
        if ("ACTION_REQUIRED".equalsIgnoreCase(ts)) {
            executeBotActionPhase(game, cur);
        }
    }

    private void executeBotActionPhase(Game game, GamePlayer botPlayer) {
        Long gid = game.getGameId();
        if (pendingOpponentLandByGameId.containsKey(gid)) {
            resolveOpponentLandForBot(game, botPlayer);
            return;
        }
        String difficulty = botDifficultyByGamePlayerId.get(botPlayer.getGamePlayerId());
        if (difficulty == null) {
            difficulty = botDifficultyByGame.getOrDefault(gid, "easy");
        }
        BoardCell cell = getCellByPosition(game, botPlayer.getPosition());
        Optional<PlayerProperty> existing =
                playerPropertyRepository.findByGame_GameIdAndBoardCell_CellId(gid, cell.getCellId());
        final Game gameRef = game;

        if (isPurchasableCell(cell) && (existing.isEmpty() || existing.get().getOwnerPlayer() == null)) {
            boolean shouldBuy = "hard".equals(difficulty) || random.nextInt(100) < 55;
            if (shouldBuy && botPlayer.getBalance() >= getCellPrice(cell)) {
                PlayerProperty pp =
                        existing.orElseGet(
                                () -> {
                                    PlayerProperty p = new PlayerProperty();
                                    p.setId(new PlayerPropertyId(gameRef.getGameId(), cell.getCellId()));
                                    p.setGame(gameRef);
                                    p.setBoardCell(cell);
                                    p.setHouseLevel(0);
                                    p.setUpgradeSpentTotal(0L);
                                    return p;
                                });
                botPlayer.setBalance(botPlayer.getBalance() - getCellPrice(cell));
                pp.setOwnerPlayer(botPlayer);
                pp.setUpgradeSpentTotal(0L);
                gamePlayerRepository.save(botPlayer);
                playerPropertyRepository.save(pp);
                long paid = getCellPrice(cell);
                String bl = cell.getName() != null ? cell.getName() : "Ô";
                logSpendOnly(
                        gid,
                        displayNameForPlayer(botPlayer)
                                + " tiêu tiền tại 「"
                                + bl
                                + "」 để mua ô trống: "
                                + formatMoneyLog(paid)
                                + ".",
                        botPlayer,
                        paid);
                return;
            }
        }
        if (existing.isPresent()
                && existing.get().getOwnerPlayer() != null
                && Objects.equals(
                        existing.get().getOwnerPlayer().getGamePlayerId(),
                        botPlayer.getGamePlayerId())) {
            int level = existing.get().getHouseLevel() == null ? 0 : existing.get().getHouseLevel();
            int max = cell.getMaxHouseLevel() == null ? 5 : cell.getMaxHouseLevel();
            long cost = upgradeCostForCell(cell);
            boolean shouldUpgrade =
                    "hard".equals(difficulty) ? random.nextInt(100) < 70 : random.nextInt(100) < 35;
            if (level < max && shouldUpgrade && botPlayer.getBalance() >= cost) {
                botPlayer.setBalance(botPlayer.getBalance() - cost);
                existing.get().setHouseLevel(level + 1);
                long sp =
                        existing.get().getUpgradeSpentTotal() == null
                                ? 0L
                                : existing.get().getUpgradeSpentTotal();
                existing.get().setUpgradeSpentTotal(sp + cost);
                gamePlayerRepository.save(botPlayer);
                playerPropertyRepository.save(existing.get());
                String bn = cell.getName() != null ? cell.getName() : "Ô";
                logSpendOnly(
                        gid,
                        displayNameForPlayer(botPlayer)
                                + " tiêu tiền tại 「"
                                + bn
                                + "」 để nâng cấp lên cấp "
                                + existing.get().getHouseLevel()
                                + ": "
                                + formatMoneyLog(cost)
                                + ".",
                        botPlayer,
                        cost);
                return;
            }
        }
        enqueueGameLog(gid, displayNameForPlayer(botPlayer) + " kết thúc lượt.");
        advanceTurn(getGame(gid));
    }

    private PlayerProperty pickLiquidationTarget(List<PlayerProperty> assets) {
        PlayerProperty withHouse = null;
        int bestLvl = -1;
        for (PlayerProperty pp : assets) {
            int lvl = pp.getHouseLevel() == null ? 0 : pp.getHouseLevel();
            if (lvl > bestLvl) {
                bestLvl = lvl;
                withHouse = pp;
            }
        }
        if (withHouse != null && bestLvl > 0) {
            return withHouse;
        }
        return assets.isEmpty() ? null : assets.get(0);
    }

    private void liquidateOneStep(Game game, GamePlayer player, PlayerProperty pp) {
        BoardCell cell = pp.getBoardCell();
        if (cell == null) {
            return;
        }
        int level = pp.getHouseLevel() == null ? 0 : pp.getHouseLevel();
        if (level > 0) {
            // Bán hết cả ô nhà: reset về level ban đầu và trả về thị trường (không còn chủ).
            long perHouseRefundNominal = Math.max(1L, upgradeCostForCell(cell) / 2);
            long refund = perHouseRefundNominal * level * LIQUIDATION_PERCENT / 100;

            pp.setHouseLevel(0);
            pp.setUpgradeSpentTotal(0L);
            pp.setOwnerPlayer(null);

            setTeamBalance(player, getTeamBalance(player) + refund);
            playerPropertyRepository.save(pp);
            return;
        }
        long mortgageNominal = getCellPrice(cell) / 2;
        long mortgageValue = mortgageNominal * LIQUIDATION_PERCENT / 100;
        pp.setOwnerPlayer(null);
        pp.setHouseLevel(0);
        pp.setUpgradeSpentTotal(0L);
        setTeamBalance(player, getTeamBalance(player) + mortgageValue);
        playerPropertyRepository.save(pp);
    }

    private long upgradeCostForCell(BoardCell cell) {
        return Math.max(UPGRADE_BASE_COST, (long) (getCellPrice(cell) * 0.5));
    }

    private long getUpgradeSpent(PlayerProperty pp, BoardCell cell) {
        if (pp.getUpgradeSpentTotal() != null && pp.getUpgradeSpentTotal() > 0) {
            return pp.getUpgradeSpentTotal();
        }
        int lv = pp.getHouseLevel() == null ? 0 : pp.getHouseLevel();
        return estimateUpgradeSpentFromLevels(cell, lv);
    }

    private long estimateUpgradeSpentFromLevels(BoardCell cell, int levels) {
        if (levels <= 0) {
            return 0L;
        }
        long per = upgradeCostForCell(cell);
        return per * levels;
    }

    private long getHouseValue(BoardCell cell, PlayerProperty pp) {
        return getCellPrice(cell) + getUpgradeSpent(pp, cell);
    }

    /** Giá bán nhà một cấp (danh nghĩa, trước phạt 10% khi gán nợ). */
    private long houseSellRefundNominal(BoardCell cell) {
        return Math.max(1L, upgradeCostForCell(cell) / 2);
    }

    private long houseSellRefund(BoardCell cell) {
        return houseSellRefundNominal(cell);
    }

    private void settleDebtPayment(Game game, GamePlayer payer, GamePlayer creditor) {
        long rent = game.getDebtRentAmount() == null ? 0L : game.getDebtRentAmount();
        Integer debtCellId = game.getDebtCellId();
        BoardCell debtCell =
                debtCellId != null
                        ? boardCellRepository.findById(debtCellId).orElse(null)
                        : null;

        long pay = rent;
        long payerBal = getTeamBalance(payer);
        setTeamBalance(payer, payerBal - pay);
        long creditorBal = getTeamBalance(creditor);
        setTeamBalance(creditor, creditorBal + pay);

        if (debtCell != null) {
            String cl = debtCell.getName() != null ? debtCell.getName() : "Ô nợ";
            logMoneyTransferThreeLines(
                    game.getGameId(),
                    displayNameForPlayer(payer)
                            + " tiêu tiền tại 「"
                            + cl
                            + "」 để trả nợ thuê cho "
                            + displayNameForPlayer(creditor)
                            + ": "
                            + formatMoneyLog(pay)
                            + ".",
                    payer,
                    creditor,
                    pay,
                    null);
            enqueueRentNotice(game, payer, creditor, debtCell, pay);
        }

        clearDebtFields(game);
        game.setTurnState("ACTION_REQUIRED");
        if (!Boolean.TRUE.equals(payer.getIsBot())) {
            game.setHumanTurnStartedAt(LocalDateTime.now());
        } else {
            game.setHumanTurnStartedAt(null);
        }
        gameRepository.save(game);
    }

    private void declareBankruptcyForDebtInternal(Game game, GamePlayer debtor, GamePlayer creditor) {
        declareBankruptcyForDebtInternal(game, debtor, creditor, true);
    }

    private void declareBankruptcyForDebtInternal(
            Game game, GamePlayer debtor, GamePlayer creditor, boolean allowTeamUp) {
        if (debtor == null || creditor == null) {
            return;
        }

        // Nếu debtor thuộc team thì coi cả team bị loại cùng nhau.
        GamePlayer teamOwner = resolveTeamOwner(debtor);
        GamePlayer teamPartner = resolveTeamPartner(teamOwner);

        // Gán thứ tự bị loại: team-owner (seq thấp) trước, phụ thuộc (seq cao) sau → phụ thuộc rank tốt hơn.
        assignEliminationOrderOnBankruptcy(game, teamOwner);
        if (teamPartner != null && !Boolean.TRUE.equals(teamPartner.getIsBankrupt())) {
            assignEliminationOrderOnBankruptcy(game, teamPartner);
        }

        // Chuyển toàn bộ tiền/tài sản của team cho chủ nợ (creditor team-owner nhận).
        long cash = teamOwner.getBalance() == null ? 0L : teamOwner.getBalance();
        GamePlayer creditorTeamOwner = resolveTeamOwner(creditor);

        setTeamBalance(creditorTeamOwner, getTeamBalance(creditorTeamOwner) + cash);

        teamOwner.setBalance(0L);
        teamOwner.setIsBankrupt(true);
        if (teamPartner != null) {
            teamPartner.setBalance(0L);
            teamPartner.setIsBankrupt(true);
        }

        transferAllPropertiesFromTo(game.getGameId(), teamOwner, creditorTeamOwner);

        gamePlayerRepository.save(teamOwner);
        if (teamPartner != null) {
            gamePlayerRepository.save(teamPartner);
        }
        clearDebtFields(game);
        gameRepository.save(game);

        if (cash > 0) {
            logMoneyTransferThreeLines(
                    game.getGameId(),
                    displayNameForPlayer(debtor)
                            + " phá sản — chuyển "
                            + formatMoneyLog(cash)
                            + " tiền mặt và tài sản cho "
                            + displayNameForPlayer(creditor)
                            + ".",
                    debtor,
                    creditorTeamOwner,
                    cash,
                    "tiền mặt khi phá sản nợ thuê");
        } else {
            enqueueGameLog(
                    game.getGameId(),
                    displayNameForPlayer(debtor)
                            + " phá sản — chuyển tài sản cho "
                            + displayNameForPlayer(creditor)
                            + ".");
        }

        // Nếu đây là lần đầu một người (solo) bị loại, cho phép creditor (chủ nợ) mời team-up.
        boolean debtorIsAlreadyInTeam = debtor.getTeamOwnerGamePlayerId() != null;
        boolean creditorIsHuman = !Boolean.TRUE.equals(creditorTeamOwner.getIsBot());
        boolean debtorIsHuman = !Boolean.TRUE.equals(debtor.getIsBot());
        boolean creditorHasPartnerAlready = creditorTeamOwner.getTeamPartnerGamePlayerId() != null;
        boolean canCreateTeamUp =
                !debtorIsAlreadyInTeam
                        && creditorIsHuman
                        && debtorIsHuman
                        && !creditorHasPartnerAlready
                        && pendingTeamUpByGameId.get(game.getGameId()) == null;

        if (allowTeamUp && canCreateTeamUp) {
            pendingTeamUpByGameId.put(
                    game.getGameId(),
                    new TeamUpPending(
                            creditorTeamOwner.getGamePlayerId(),
                            debtor.getGamePlayerId(),
                            TURN_STATE_TEAMUP_INVITE));
            game.setTurnState(TURN_STATE_TEAMUP_INVITE);
            // Chuyển lượt sang đội trưởng (creditor) để người này ra quyết định.
            game.setCurrentPlayerOrder(creditorTeamOwner.getTurnOrder());
            game.setHumanTurnStartedAt(LocalDateTime.now());
            gameRepository.save(game);
            return;
        }

        advanceTurn(game);
    }

    @Transactional
    public GameActionResponse sellAssetForDebt(Long gameId, Long accountId, DebtSellRequest request) {
        Game game = getGame(gameId);
        if (!"INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Bạn không đang nợ tiền thuê");
        }
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);
        ensureNoTeamUpPending(gameId);
        if (request == null || request.getCellId() == null) {
            throw new RuntimeException("Cần cellId của tài sản cần bán");
        }
        PlayerProperty pp =
                playerPropertyRepository
                        .findByGame_GameIdAndBoardCell_CellId(gameId, request.getCellId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy ô này"));
        Long effectiveTeamOwnerId = resolveTeamOwnerGamePlayerId(player);
        if (pp.getOwnerPlayer() == null
                || !Objects.equals(pp.getOwnerPlayer().getGamePlayerId(), effectiveTeamOwnerId)) {
            throw new RuntimeException("Không phải tài sản của bạn");
        }
        liquidateOneStep(game, player, pp);
        player = gamePlayerRepository.findById(player.getGamePlayerId()).orElseThrow();
        game = getGame(gameId);
        GamePlayer creditor =
                gamePlayerRepository
                        .findById(game.getDebtCreditorGamePlayerId())
                        .orElseThrow();
        long owed = game.getDebtRentAmount() == null ? 0L : game.getDebtRentAmount();
        if (getTeamBalance(player) >= owed) {
            settleDebtPayment(game, player, creditor);
        }
        return actionResult(gameId, "Đã xử lý tài sản", accountId);
    }

    @Transactional
    public GameActionResponse declareBankruptcyForDebt(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        if (!"INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Bạn không đang nợ tiền thuê");
        }
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);
        ensureNoTeamUpPending(gameId);
        GamePlayer creditor =
                gamePlayerRepository
                        .findById(game.getDebtCreditorGamePlayerId())
                        .orElseThrow();
        declareBankruptcyForDebtInternal(game, player, creditor);
        return actionResult(gameId, "Bạn đã phá sản — chuyển tài sản cho chủ nợ", accountId);
    }

    /**
     * Đầu hàng: khi đang nợ và tới lượt — tương đương phá sản; còn không — bỏ hết tài sản, bị loại, có thể kết thúc ván.
     */
    @Transactional
    public GameActionResponse surrenderGame(Long gameId, Long accountId) {
        Game game = getGame(gameId);
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new RuntimeException("Ván đã kết thúc");
        }
        ensureNoTeamUpPending(gameId);
        UserProfile profile = getProfileByAccountId(accountId);
        GamePlayer player =
                gamePlayerRepository
                        .findByGameIdAndUserProfileId(gameId, profile.getUserProfileId())
                        .orElseThrow(() -> new RuntimeException("Bạn không tham gia ván này"));
        if (Boolean.TRUE.equals(player.getIsBot())) {
            throw new RuntimeException("Không áp dụng cho bot");
        }
        if (Boolean.TRUE.equals(player.getIsBankrupt())) {
            throw new RuntimeException("Bạn đã bị loại");
        }

        if (Boolean.TRUE.equals(game.getSoloVsAi())) {
            return finishSoloVsAiSurrender(gameId, game, player, accountId);
        }

        if ("INSOLVENT".equalsIgnoreCase(game.getTurnState())
                && Objects.equals(game.getCurrentPlayerOrder(), player.getTurnOrder())) {
            GamePlayer creditor =
                    gamePlayerRepository
                            .findById(game.getDebtCreditorGamePlayerId())
                            .orElseThrow();
            declareBankruptcyForDebtInternal(game, player, creditor, false);
            return actionResult(gameId, "Bạn đã phá sản — chuyển tài sản cho chủ nợ", accountId);
        }

        boolean isCurrent = Objects.equals(game.getCurrentPlayerOrder(), player.getTurnOrder());
        if (isCurrent) {
            pendingOpponentLandByGameId.remove(gameId);
        }

        GamePlayer teamOwner = resolveTeamOwner(player);
        GamePlayer teamPartner = resolveTeamPartner(teamOwner);

        assignEliminationOrderOnBankruptcy(game, teamOwner);
        if (teamPartner != null && !Boolean.TRUE.equals(teamPartner.getIsBankrupt())) {
            assignEliminationOrderOnBankruptcy(game, teamPartner);
        }
        releaseAllPropertiesUnowned(gameId, teamOwner);
        teamOwner.setBalance(0L);
        teamOwner.setIsBankrupt(true);
        gamePlayerRepository.save(teamOwner);
        if (teamPartner != null) {
            teamPartner.setBalance(0L);
            teamPartner.setIsBankrupt(true);
            gamePlayerRepository.save(teamPartner);
        }
        enqueueGameLog(
                gameId,
                displayNameForPlayer(player) + " đầu hàng (phá sản) — rời ván, tài sản trả về thị trường.");

        game = getGame(gameId);
        if (isCurrent) {
            advanceTurn(game);
        } else {
            checkGameFinishedAfterElimination(getGame(gameId));
        }
        return actionResult(gameId, "Bạn đã đầu hàng.", accountId);
    }

    /** Đấu máy: đầu hàng = kết thúc ván ngay, xếp hạng theo số dư hiện tại (không thưởng xu). */
    private GameActionResponse finishSoloVsAiSurrender(
            Long gameId, Game game, GamePlayer triggeringPlayer, Long accountId) {
        List<GamePlayer> all = new ArrayList<>(gamePlayerRepository.findByGameIdOrderByTurnOrderAsc(gameId));
        all.sort(Comparator.comparingLong(p -> p.getBalance() == null ? 0L : p.getBalance()));
        GamePlayer winner = all.get(all.size() - 1);
        int seq = 0;
        for (GamePlayer p : all) {
            if (Objects.equals(p.getGamePlayerId(), winner.getGamePlayerId())) {
                p.setEliminationOrder(null);
                p.setIsBankrupt(false);
            } else {
                seq++;
                p.setEliminationOrder(seq);
                p.setIsBankrupt(true);
            }
            gamePlayerRepository.save(p);
        }
        game.setStatus(GameStatus.FINISHED);
        game.setTurnState("END_TURN");
        game.setWinnerPlayerId(winner.getGamePlayerId());
        game.setEndedAt(LocalDateTime.now());
        game.setHumanTurnStartedAt(null);
        gameRepository.save(game);
        releaseRoomAfterGameFinished(gameId);
        enqueueGameLog(
                gameId,
                displayNameForPlayer(triggeringPlayer) + " đầu hàng — kết thúc ván đấu máy (xếp hạng theo số dư).");
        return actionResult(gameId, "Ván đấu máy kết thúc — xếp hạng theo tiền hiện có.", accountId);
    }

    private void releaseAllPropertiesUnowned(Long gameId, GamePlayer owner) {
        List<PlayerProperty> list =
                playerPropertyRepository.findByGame_GameIdAndOwnerPlayer_GamePlayerId(
                        gameId, owner.getGamePlayerId());
        for (PlayerProperty pp : list) {
            pp.setOwnerPlayer(null);
            pp.setHouseLevel(0);
            pp.setUpgradeSpentTotal(0L);
            playerPropertyRepository.save(pp);
        }
    }

    /** Khi người bị loại không phải người đang tới lượt — chỉ kiểm tra còn ≤1 người hoạt động. */
    private void checkGameFinishedAfterElimination(Game game) {
        game = getGame(game.getGameId());
        finishGameIfAtMostOneActivePlayer(game);
    }

    /**
     * Còn tối đa 1 người chưa phá sản → kết thúc ván (gọi đầu lượt / sau mỗi loại / khi poll state).
     */
    private boolean finishGameIfAtMostOneActivePlayer(Game game) {
        if (game.getStatus() != GameStatus.PLAYING) {
            return false;
        }
        List<GamePlayer> all = gamePlayerRepository.findByGameIdOrderByTurnOrderAsc(game.getGameId());
        List<GamePlayer> active = all.stream().filter(p -> !Boolean.TRUE.equals(p.getIsBankrupt())).toList();
        if (active.isEmpty()) {
            game.setStatus(GameStatus.FINISHED);
            game.setTurnState("END_TURN");
            game.setWinnerPlayerId(null);
            game.setEndedAt(LocalDateTime.now());
            game.setHumanTurnStartedAt(null);
            gameRepository.save(game);
            releaseRoomAfterGameFinished(game.getGameId());
            awardEndMatchCoins(game.getGameId());
            return true;
        }

        // Với team-up: coi team (2 người) là 1 đơn vị thắng.
        Set<Long> activeTeams =
                active.stream()
                        .map(this::resolveTeamOwnerGamePlayerId)
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toSet());

        if (activeTeams.size() > 1) {
            return false;
        }
        game.setStatus(GameStatus.FINISHED);
        game.setTurnState("END_TURN");
        game.setWinnerPlayerId(activeTeams.iterator().next());
        game.setEndedAt(LocalDateTime.now());
        game.setHumanTurnStartedAt(null);
        gameRepository.save(game);
        releaseRoomAfterGameFinished(game.getGameId());
        awardEndMatchCoins(game.getGameId());
        return true;
    }

    /** Phòng quay về chờ để «Chơi lại» không bị redirect vào bàn cũ (activeGameId + IN_GAME). */
    private void releaseRoomAfterGameFinished(Long gameId) {
        roomRepository
                .findByActiveGameId(gameId)
                .ifPresent(
                        room -> {
                            room.setStatus(RoomStatus.WAITING);
                            room.setActiveGameId(null);
                            room.setIsStarted(false);
                            roomRepository.save(room);
                            for (RoomPlayer rp :
                                    roomPlayerRepository.findByRoom_RoomIdOrderBySlotIndexAsc(
                                            room.getRoomId())) {
                                rp.setIsPlaying(false);
                                rp.setIsReady(false);
                                roomPlayerRepository.save(rp);
                            }
                        });
    }

    private GameStateResponse.DebtSituationDto buildDebtSituation(Game game, GamePlayer current) {
        if (!"INSOLVENT".equalsIgnoreCase(game.getTurnState())
                || game.getDebtRentAmount() == null
                || game.getDebtCreditorGamePlayerId() == null) {
            return null;
        }
        GamePlayer creditor =
                gamePlayerRepository.findById(game.getDebtCreditorGamePlayerId()).orElse(null);
        if (creditor == null) {
            return null;
        }
        BoardCell cause =
                game.getDebtCellId() != null
                        ? boardCellRepository.findById(game.getDebtCellId()).orElse(null)
                        : null;
        String causeName = cause != null && cause.getName() != null ? cause.getName() : "Ô";

        Long assetOwnerId = resolveTeamOwnerGamePlayerId(current);
        List<PlayerProperty> mine =
                playerPropertyRepository.findByGame_GameIdAndOwnerPlayer_GamePlayerId(
                        game.getGameId(), assetOwnerId);
        List<GameStateResponse.DebtAssetDto> assets = new ArrayList<>();
        List<BoardCell> ordered = listBoardCellsInPlayOrder(game);
        for (PlayerProperty pp : mine) {
            if (pp.getBoardCell() == null) {
                continue;
            }
            BoardCell bc = pp.getBoardCell();
            int bi = 0;
            for (int i = 0; i < ordered.size(); i++) {
                if (ordered.get(i).getCellId().equals(bc.getCellId())) {
                    bi = i;
                    break;
                }
            }
            int hl = pp.getHouseLevel() == null ? 0 : pp.getHouseLevel();
            String action = hl > 0 ? "SELL_HOUSES" : "MORTGAGE";
            long perUnitNominal = hl > 0 ? houseSellRefundNominal(bc) : getCellPrice(bc) / 2;
            long cash =
                    (hl > 0 ? perUnitNominal * hl : perUnitNominal)
                            * LIQUIDATION_PERCENT
                            / 100;
            assets.add(
                    GameStateResponse.DebtAssetDto.builder()
                            .cellId(bc.getCellId())
                            .name(bc.getName())
                            .boardIndex(bi)
                            .houseLevel(hl)
                            .suggestedAction(action)
                            .cashIfSold(cash)
                            .build());
        }

        return GameStateResponse.DebtSituationDto.builder()
                .amountOwed(game.getDebtRentAmount())
                .creditorTurnOrder(creditor.getTurnOrder())
                .creditorName(displayNameForPlayer(creditor))
                .causeCellName(causeName)
                .assets(assets)
                .build();
    }

    private long rentBaseLandOnly(BoardCell cell) {
        return cell.getBaseRent() == null ? Math.max(20, getCellPrice(cell) / 5) : cell.getBaseRent();
    }

    /** Thuê = (giá nhà × cấp nhà × 10%); cấp 0 = thuê đất cơ bản. */
    private long calculateRent(BoardCell cell, PlayerProperty property) {
        int level = property.getHouseLevel() == null ? 0 : property.getHouseLevel();
        if (level <= 0) {
            return rentBaseLandOnly(cell);
        }
        long houseValue = getHouseValue(cell, property);
        return (houseValue * level) / 10;
    }

    private void advanceTurn(Game game) {
        if (finishGameIfAtMostOneActivePlayer(game)) {
            return;
        }
        List<GamePlayer> players = gamePlayerRepository.findByGameIdOrderByTurnOrderAsc(game.getGameId());
        if (players.isEmpty()) {
            throw new RuntimeException("Game has no players");
        }

        GamePlayer current = getCurrentTurnPlayer(game);

        // Rule team-up: nếu đang là đội trưởng thì lượt kế tiếp phải là phụ thuộc (nếu phụ thuộc còn hoạt động).
        GamePlayer forcedPartner = null;
        if (current != null && !Boolean.TRUE.equals(current.getIsBankrupt())) {
            // Chỉ áp dụng "lượt phụ thuộc ngay sau chủ team" khi current thực sự là đội trưởng.
            if (!isTeamDependent(current)) {
                GamePlayer partner = resolveTeamPartner(current);
                if (partner != null && !Boolean.TRUE.equals(partner.getIsBankrupt())) {
                    forcedPartner =
                            gamePlayerRepository.findById(partner.getGamePlayerId()).orElse(null);
                    if (forcedPartner != null && Boolean.TRUE.equals(forcedPartner.getIsBankrupt())) {
                        forcedPartner = null;
                    }
                }
            }
        }

        GamePlayer nextPlayer = null;
        if (forcedPartner != null) {
            nextPlayer = forcedPartner;
        } else {
            int currentOrder = game.getCurrentPlayerOrder() == null ? 1 : game.getCurrentPlayerOrder();
            int nextOrder = currentOrder;
            for (int i = 0; i < players.size(); i++) {
                nextOrder = nextOrder >= players.size() ? 1 : nextOrder + 1;
                GamePlayer candidate = null;
                for (GamePlayer p : players) {
                    if (Objects.equals(p.getTurnOrder(), nextOrder)) {
                        candidate = p;
                        break;
                    }
                }
                if (candidate != null && !Boolean.TRUE.equals(candidate.getIsBankrupt())) {
                    nextPlayer = candidate;
                    break;
                }
            }
        }

        if (nextPlayer == null || nextPlayer.getTurnOrder() == null) {
            throw new RuntimeException("Không tìm thấy người chơi tiếp theo trong hàng đợi lượt");
        }

        game.setCurrentPlayerOrder(nextPlayer.getTurnOrder());
        game.setCurrentTurn((game.getCurrentTurn() == null ? 1 : game.getCurrentTurn()) + 1);
        game.setTurnState("WAIT_ROLL");

        Integer cd = nextPlayer.getSkillCooldownRemaining();
        if (cd != null && cd > 0) {
            nextPlayer.setSkillCooldownRemaining(cd - 1);
            gamePlayerRepository.save(nextPlayer);
        }
        if (!Boolean.TRUE.equals(nextPlayer.getIsBot())) {
            game.setHumanTurnStartedAt(LocalDateTime.now());
        } else {
            game.setHumanTurnStartedAt(null);
        }
        gameRepository.save(game);
    }

    /**
     * Ô bàn cờ theo vị trí (0..n-1): ưu tiên thứ tự {@link MapCell} của map trong game,
     * nếu chưa cấu hình map thì dùng {@link BoardCell} trong DB sắp xếp theo {@code cellId}.
     * Luôn trả về entity đã persist để FK {@code PlayerProperty} hợp lệ.
     */
    private BoardCell getCellByPosition(Game game, Integer position) {
        List<BoardCell> ordered = listBoardCellsInPlayOrder(game);
        int safePos = (position == null ? 0 : position) % ordered.size();
        return ordered.get(safePos);
    }

    private List<BoardCell> listBoardCellsInPlayOrder(Game game) {
        Integer mapId = game.getMapId() != null ? game.getMapId() : 1;
        List<MapCell> mapCells = mapCellRepository.findAllByMapIdOrderByPositionWithCell(mapId);
        if (mapCells.size() < BoardClassicMapBootstrapService.EXPECTED_CELLS) {
            boardClassicMapBootstrapService.ensureClassicBoardIfMissing();
            mapCells = mapCellRepository.findAllByMapIdOrderByPositionWithCell(mapId);
        }
        if (!mapCells.isEmpty()) {
            List<BoardCell> out = new ArrayList<>(mapCells.size());
            for (MapCell mc : mapCells) {
                if (mc.getBoardCell() != null) {
                    out.add(mc.getBoardCell());
                }
            }
            if (!out.isEmpty()) {
                return out;
            }
        }
        List<BoardCell> cells = boardCellRepository.findAll(Sort.by(Sort.Direction.ASC, "cellId"));
        if (cells.isEmpty()) {
            throw new RuntimeException(
                    "Bàn cờ chưa có dữ liệu ô (BoardCell). Kiểm tra file classpath seed/board-classic.json hoặc static/seed/board-classic.json.");
        }
        return cells;
    }

    private int getBoardCellCount(Game game) {
        return listBoardCellsInPlayOrder(game).size();
    }

    private long getCellPrice(BoardCell cell) {
        return cell.getPrice() == null ? 200L : Math.max(cell.getPrice(), 1);
    }

    private boolean isPurchasableCell(BoardCell cell) {
        if (cell.getName() != null && "GO".equalsIgnoreCase(cell.getName().trim())) {
            return false;
        }
        if (cell.getType() == null) {
            return true;
        }
        String type = cell.getType().toUpperCase(Locale.ROOT);
        if (type.contains("START") || "GO".equals(type)) {
            return false;
        }
        return type.contains("PROPERTY") || type.contains("LAND");
    }

    private void validateHumanTurn(GamePlayer currentTurnPlayer, Long accountId) {
        UserProfile profile = getProfileByAccountId(accountId);
        if (Boolean.TRUE.equals(currentTurnPlayer.getIsBot())) {
            throw new RuntimeException("Đây là lượt của bot");
        }
        if (!Objects.equals(currentTurnPlayer.getUserProfileId(), profile.getUserProfileId())) {
            throw new RuntimeException("Không phải lượt của bạn");
        }
    }

    private GamePlayer getCurrentTurnPlayer(Game game) {
        Integer order = game.getCurrentPlayerOrder();
        if (order == null) {
            throw new RuntimeException("Game currentPlayerOrder is missing");
        }
        return gamePlayerRepository.findByGameIdAndTurnOrder(game.getGameId(), order)
                .orElseThrow(() -> new RuntimeException("Current turn player not found"));
    }

    private Game getGame(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
    }

    private UserProfile getProfileByAccountId(Long accountId) {
        if (accountId == null) {
            throw new RuntimeException("Missing X-Account-Id header");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));
    }

    private String normalizeDifficulty(String raw) {
        if (raw == null) return "easy";
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "hard".equals(value) ? "hard" : "easy";
    }

    private GameActionResponse actionResult(Long gameId, String message, Long accountId) {
        return GameActionResponse.builder()
                .message(message)
                .state(getState(gameId, accountId))
                .build();
    }

    private void ensureHumanTurnClockStarted(Game game) {
        if (game.getStatus() != GameStatus.PLAYING) {
            return;
        }
        GamePlayer current = getCurrentTurnPlayer(game);
        if (Boolean.TRUE.equals(current.getIsBot())) {
            return;
        }
        if (game.getHumanTurnStartedAt() == null) {
            game.setHumanTurnStartedAt(LocalDateTime.now());
            gameRepository.save(game);
        }
    }

    /**
     * Trạng thái INSOLVENT nhưng không còn gì để thanh khoản — tự phá sản để tránh kẹt UI / DB.
     */
    private void healStuckInsolvencyIfNeeded(Game game) {
        if (game.getStatus() != GameStatus.PLAYING) {
            return;
        }
        if (!"INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            return;
        }
        GamePlayer cur = getCurrentTurnPlayer(game);
        Long assetOwnerId = resolveTeamOwnerGamePlayerId(cur);
        List<PlayerProperty> mine =
                playerPropertyRepository.findByGame_GameIdAndOwnerPlayer_GamePlayerId(
                        game.getGameId(), assetOwnerId);
        if (canLiquidateAny(mine)) {
            return;
        }
        Long credId = game.getDebtCreditorGamePlayerId();
        if (credId == null) {
            clearDebtFields(game);
            game.setTurnState("ACTION_REQUIRED");
            gameRepository.save(game);
            return;
        }
        GamePlayer creditor = gamePlayerRepository.findById(credId).orElse(null);
        if (creditor == null) {
            clearDebtFields(game);
            game.setTurnState("ACTION_REQUIRED");
            gameRepository.save(game);
            return;
        }
        declareBankruptcyForDebtInternal(game, cur, creditor);
    }

    private void maybeResolveExpiredHumanTurn(Game game) {
        if (game.getStatus() != GameStatus.PLAYING) {
            return;
        }
        GamePlayer cur = getCurrentTurnPlayer(game);
        if (Boolean.TRUE.equals(cur.getIsBot())) {
            return;
        }
        if (game.getHumanTurnStartedAt() == null) {
            return;
        }
        String ts = game.getTurnState();
        int limit;
        if ("WAIT_ROLL".equalsIgnoreCase(ts)) {
            limit = HUMAN_WAIT_ROLL_SECONDS;
        } else if ("ACTION_REQUIRED".equalsIgnoreCase(ts) || "INSOLVENT".equalsIgnoreCase(ts)) {
            limit = HUMAN_ACTION_SECONDS;
        } else {
            return;
        }
        long elapsed = ChronoUnit.SECONDS.between(game.getHumanTurnStartedAt(), LocalDateTime.now());
        if (elapsed < limit) {
            return;
        }
        if ("INSOLVENT".equalsIgnoreCase(ts)) {
            GamePlayer creditor =
                    gamePlayerRepository
                            .findById(game.getDebtCreditorGamePlayerId())
                            .orElse(null);
            if (creditor != null) {
                declareBankruptcyForDebtInternal(game, cur, creditor);
            }
            return;
        }
        if ("WAIT_ROLL".equalsIgnoreCase(ts)) {
            performRollAndMove(game, cur);
        } else {
            pendingOpponentLandByGameId.remove(game.getGameId());
            advanceTurn(game);
        }
    }

    /** Hết giờ lượt người: mất kết nối lâu → một bước AI; còn kết nối → hết timer mới tự xử lý. */
    private void resolveHumanTurnAutomation(Game game) {
        if (game.getStatus() != GameStatus.PLAYING) {
            return;
        }
        GamePlayer cur = getCurrentTurnPlayer(game);
        if (Boolean.TRUE.equals(cur.getIsBot())) {
            return;
        }
        Long accId = resolveAccountIdForGamePlayer(cur);
        if (presenceRegistry.isDisconnectedLongerThan(accId, DISCONNECT_SUBSTITUTE_AFTER_MS)) {
            runDisconnectedSubstituteStep(game, cur);
        } else {
            maybeResolveExpiredHumanTurn(game);
        }
    }

    private Long resolveAccountIdForGamePlayer(GamePlayer p) {
        if (p == null || p.getUserProfileId() == null) {
            return null;
        }
        return userProfileRepository
                .findById(p.getUserProfileId())
                .map(UserProfile::getAccount)
                .map(Account::getAccountId)
                .orElse(null);
    }

    /**
     * Một bước giống bot cho người đang «offline» quá ngưỡng — khi họ gọi API lại (touch), nhánh này ngừng.
     */
    private void runDisconnectedSubstituteStep(Game game, GamePlayer cur) {
        Long gid = game.getGameId();
        game = getGame(gid);
        cur = gamePlayerRepository.findById(cur.getGamePlayerId()).orElseThrow();
        if (game.getStatus() != GameStatus.PLAYING) {
            return;
        }
        if (Boolean.TRUE.equals(cur.getIsBot())) {
            return;
        }
        String ts = game.getTurnState();
        if ("INSOLVENT".equalsIgnoreCase(ts)) {
            advanceOneBotDebtStep(game, cur);
            return;
        }
        if ("WAIT_ROLL".equalsIgnoreCase(ts)) {
            enqueueGameLog(
                    gid,
                    displayNameForPlayer(cur) + " mất kết nối — hệ thống tung xúc xắc thay.");
            performRollAndMove(game, cur);
            return;
        }
        if ("ACTION_REQUIRED".equalsIgnoreCase(ts)) {
            enqueueGameLog(
                    gid,
                    displayNameForPlayer(cur) + " mất kết nối — hệ thống hành động thay (một bước).");
            executeBotActionPhase(game, cur);
        }
    }

    private Integer computeTurnSecondsRemaining(Game game, GamePlayer currentPlayer) {
        if (game.getStatus() != GameStatus.PLAYING || Boolean.TRUE.equals(currentPlayer.getIsBot())) {
            return null;
        }
        if (game.getHumanTurnStartedAt() == null) {
            return null;
        }
        String ts = game.getTurnState();
        int limit;
        if ("WAIT_ROLL".equalsIgnoreCase(ts)) {
            limit = HUMAN_WAIT_ROLL_SECONDS;
        } else if ("ACTION_REQUIRED".equalsIgnoreCase(ts) || "INSOLVENT".equalsIgnoreCase(ts)) {
            limit = HUMAN_ACTION_SECONDS;
        } else {
            return null;
        }
        long elapsed = ChronoUnit.SECONDS.between(game.getHumanTurnStartedAt(), LocalDateTime.now());
        return (int) Math.max(0, limit - elapsed);
    }

    private Boolean computeMyTurn(Long accountId, GamePlayer currentPlayer) {
        if (accountId == null || Boolean.TRUE.equals(currentPlayer.getIsBot())) {
            return accountId == null ? null : Boolean.FALSE;
        }
        try {
            UserProfile profile = getProfileByAccountId(accountId);
            return Objects.equals(currentPlayer.getUserProfileId(), profile.getUserProfileId());
        } catch (RuntimeException ignored) {
            return Boolean.FALSE;
        }
    }

    private Integer resolveMyPlayerTurnOrder(Long accountId, List<GamePlayer> playersInGame) {
        if (accountId == null) {
            return null;
        }
        try {
            UserProfile profile = getProfileByAccountId(accountId);
            for (GamePlayer p : playersInGame) {
                if (Objects.equals(p.getUserProfileId(), profile.getUserProfileId())) {
                    return p.getTurnOrder();
                }
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private String displayNameForPlayer(GamePlayer player) {
        if (Boolean.TRUE.equals(player.getIsBot())) {
            if (player.getCharacterId() != null) {
                return heroRepository
                        .findById(player.getCharacterId())
                        .map(h -> "Bot · " + h.getName())
                        .orElseGet(() -> "Bot " + (player.getTurnOrder() != null ? player.getTurnOrder() : ""));
            }
            return "Bot " + (player.getTurnOrder() != null ? player.getTurnOrder() : "");
        }
        if (player.getUserProfileId() != null) {
            UserProfile profile = userProfileRepository.findById(player.getUserProfileId()).orElse(null);
            if (profile != null && profile.getUsername() != null && !profile.getUsername().isBlank()) {
                return profile.getUsername();
            }
        }
        return "Người chơi " + (player.getTurnOrder() != null ? player.getTurnOrder() : "");
    }

    private GameStateResponse.PlayerStateDto toPlayerState(GamePlayer player) {
        String username = displayNameForPlayer(player);
        String avatarUrl = null;
        String heroImageUrl = null;
        String heroName = null;

        if (!Boolean.TRUE.equals(player.getIsBot()) && player.getUserProfileId() != null) {
            UserProfile profile = userProfileRepository.findById(player.getUserProfileId()).orElse(null);
            if (profile != null) {
                avatarUrl = profile.getAvatarUrl();
            }
        }
        if (player.getCharacterId() != null) {
            Optional<Hero> ho = heroRepository.findById(player.getCharacterId());
            if (ho.isPresent()) {
                heroName = ho.get().getName();
                heroImageUrl = null;
            }
        }

        List<GameStateResponse.PlayerSkillDto> skills = playerSkillViewService.buildSkillDtos(player);

        Integer teamOwnerTurnOrder = null;
        GamePlayer teamOwner = resolveTeamOwner(player);
        if (teamOwner != null) {
            teamOwnerTurnOrder = teamOwner.getTurnOrder();
        }

        return GameStateResponse.PlayerStateDto.builder()
                .gamePlayerId(player.getGamePlayerId())
                .userProfileId(player.getUserProfileId())
                .turnOrder(player.getTurnOrder())
                .position(player.getPosition())
                .balance(getTeamBalance(player))
                .isBot(player.getIsBot())
                .isBankrupt(player.getIsBankrupt())
                .teamOwnerTurnOrder(teamOwnerTurnOrder)
                .username(username)
                .avatarUrl(avatarUrl)
                .heroImageUrl(heroImageUrl)
                .heroName(heroName)
                .inJail(Boolean.TRUE.equals(player.getInJail()))
                .jailFailedRolls(player.getJailFailedRolls() == null ? 0 : player.getJailFailedRolls())
                .skills(skills)
                .build();
    }

    private GameStateResponse.CellInfoDto toCellInfo(BoardCell cell, PlayerProperty property, GamePlayer currentPlayer, String turnState) {
        Long ownerGamePlayerId = null;
        Integer ownerTurnOrder = null;
        Integer houseLevel = 0;
        if (property != null) {
            houseLevel = property.getHouseLevel() == null ? 0 : property.getHouseLevel();
            if (property.getOwnerPlayer() != null) {
                ownerGamePlayerId = property.getOwnerPlayer().getGamePlayerId();
                ownerTurnOrder = property.getOwnerPlayer().getTurnOrder();
            }
        }

        long price = getCellPrice(cell);
        long upgradeCost = upgradeCostForCell(cell);
        Long houseValue =
                property != null && property.getOwnerPlayer() != null
                        ? getHouseValue(cell, property)
                        : null;
        long estimatedRent;
        if (property != null && property.getOwnerPlayer() != null) {
            estimatedRent = calculateRent(cell, property);
        } else {
            estimatedRent = rentBaseLandOnly(cell);
        }
        boolean insolvent = "INSOLVENT".equalsIgnoreCase(turnState);
        boolean actionPhase = "ACTION_REQUIRED".equalsIgnoreCase(turnState);

        long teamBalance = getTeamBalance(currentPlayer);
        Long effectiveTeamOwnerId = resolveTeamOwnerGamePlayerId(currentPlayer);

        boolean canBuy = !insolvent && actionPhase
                && isPurchasableCell(cell)
                && (property == null || property.getOwnerPlayer() == null)
                && teamBalance >= price;
        boolean canUpgrade = !insolvent && actionPhase
                && property != null
                && property.getOwnerPlayer() != null
                && Objects.equals(property.getOwnerPlayer().getGamePlayerId(), effectiveTeamOwnerId)
                && teamBalance >= upgradeCost
                && (cell.getMaxHouseLevel() == null || houseLevel < cell.getMaxHouseLevel());

        return GameStateResponse.CellInfoDto.builder()
                .cellId(cell.getCellId())
                .name(cell.getName())
                .type(cell.getType())
                .price(price)
                .houseValue(houseValue)
                .upgradeCost(upgradeCost)
                .estimatedRent(estimatedRent)
                .ownerGamePlayerId(ownerGamePlayerId)
                .ownerTurnOrder(ownerTurnOrder)
                .houseLevel(houseLevel)
                .purchasable(isPurchasableCell(cell))
                .canBuy(canBuy)
                .canUpgrade(canUpgrade)
                .build();
    }
}
