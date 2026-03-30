package com.game.monopoly.service.skill;

import com.game.monopoly.dto.SkillActivateRequest;
import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.model.inGameData.Game;
import com.game.monopoly.model.inGameData.GamePlayer;
import com.game.monopoly.model.inGameData.PlayerProperty;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.BoardCell;
import com.game.monopoly.model.metaData.Skill;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.BoardCellRepository;
import com.game.monopoly.repository.CharacterSkillRepository;
import com.game.monopoly.repository.GamePlayerRepository;
import com.game.monopoly.repository.GameRepository;
import com.game.monopoly.repository.PlayerPropertyRepository;
import com.game.monopoly.repository.SkillRepository;
import com.game.monopoly.repository.UserProfileRepository;
import com.game.monopoly.service.GamePlayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Kích hoạt kỹ năng chủ động trong ván — hiệu ứng chi tiết theo {@link Skill#getEffectType()} (bổ sung dần).
 */
@Service
@RequiredArgsConstructor
public class SkillActivationService {

    private final ObjectProvider<GamePlayService> gamePlayService;
    private final GameRepository gameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final CharacterSkillRepository characterSkillRepository;
    private final SkillRepository skillRepository;
    private final UserProfileRepository userProfileRepository;
    private final AccountRepository accountRepository;
    private final PlayerPropertyRepository playerPropertyRepository;
    private final BoardCellRepository boardCellRepository;

    @Transactional
    public String performActivate(
            Long gameId,
            Long accountId,
            SkillActivateRequest request,
            Consumer<String> gameLogSink) {
        if (request == null || request.getSkillId() == null) {
            throw new RuntimeException("Cần skillId");
        }
        Game game =
                gameRepository
                        .findById(gameId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy game"));
        if (game.getStatus() != GameStatus.PLAYING) {
            throw new RuntimeException("Game không đang chơi");
        }
        GamePlayer player = getCurrentTurnPlayer(game);
        validateHumanTurn(player, accountId);
        if ("INSOLVENT".equalsIgnoreCase(game.getTurnState())) {
            throw new RuntimeException("Đang nợ — không dùng kỹ năng");
        }
        String ts = game.getTurnState() == null ? "" : game.getTurnState();
        if (!"WAIT_ROLL".equalsIgnoreCase(ts) && !"ACTION_REQUIRED".equalsIgnoreCase(ts)) {
            throw new RuntimeException("Không thể dùng kỹ năng lúc này");
        }

        if (player.getCharacterId() == null) {
            throw new RuntimeException("Bạn chưa chọn nhân vật");
        }
        Skill skill =
                skillRepository
                        .findById(request.getSkillId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy kỹ năng"));

        boolean owns =
                characterSkillRepository.findByHero_CharacterId(player.getCharacterId()).stream()
                        .anyMatch(
                                cs ->
                                        cs.getSkill() != null
                                                && Objects.equals(
                                                        cs.getSkill().getSkillId(), skill.getSkillId()));
        if (!owns) {
            throw new RuntimeException("Nhân vật của bạn không có kỹ năng này");
        }

        boolean passive =
                skill.getTriggerType() != null
                        && skill.getTriggerType().toUpperCase(Locale.ROOT).contains("PASSIVE");
        if (passive) {
            throw new RuntimeException("Kỹ năng thụ động không cần kích hoạt");
        }

        int cdRem =
                player.getSkillCooldownRemaining() == null ? 0 : player.getSkillCooldownRemaining();
        if (cdRem > 0) {
            throw new RuntimeException("Kỹ năng đang hồi (" + cdRem + " lượt)");
        }

        String effEarly = skill.getEffectType() == null ? "" : skill.getEffectType().toUpperCase(Locale.ROOT);
        if ("DUEL_DICE".equals(effEarly) && !"WAIT_ROLL".equalsIgnoreCase(ts)) {
            throw new RuntimeException("Thần xúc xắc chỉ dùng khi đang chờ tung xúc xắc.");
        }

        String detail = applyEffect(skill, player, game, request, accountId);

        int cd = skill.getCooldown() == null ? 0 : skill.getCooldown();
        player.setSkillCooldownRemaining(Math.max(0, cd));
        gamePlayerRepository.save(player);

        if (gameLogSink != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(displayNameForPlayer(player))
                    .append(" kích hoạt \"")
                    .append(skill.getName())
                    .append("\"");
            if (detail != null && !detail.isBlank()) {
                sb.append(" — ").append(detail);
            } else {
                sb.append(".");
            }
            gameLogSink.accept(sb.toString());
        }

        return "Đã kích hoạt " + skill.getName();
    }

    /** Hook hiệu ứng — mở rộng theo {@link Skill#getEffectType()} và luật GamePlayService. */
    private String applyEffect(
            Skill skill, GamePlayer player, Game game, SkillActivateRequest request, Long accountId) {
        String effect =
                skill.getEffectType() == null ? "" : skill.getEffectType().toUpperCase(Locale.ROOT);
        switch (effect) {
            case "RESET_PROPERTY_OWNER":
                return applyResetPropertyOwner(game, player, request);
            case "MARK_AND_BUYBACK":
                return applyMarkAndBuyback(skill, game, player, request);
            case "DUEL_DICE":
                return applyDuelDice(game, player, request, accountId);
            case "SET_MOVE_RANGE":
            case "EXTRA_RANDOM_MOVE":
                return "";
            default:
                return "";
        }
    }

    private String applyDuelDice(Game game, GamePlayer player, SkillActivateRequest request, Long accountId) {
        if (request.getDice1() == null || request.getDice2() == null) {
            throw new RuntimeException("Chọn giá trị hai xúc xắc (1–6)");
        }
        int a = request.getDice1();
        int b = request.getDice2();
        if (a < 1 || a > 6 || b < 1 || b > 6) {
            throw new RuntimeException("Mỗi xúc xắc từ 1 đến 6");
        }
        return gamePlayService.getObject().applyChosenDiceRollForSkill(game.getGameId(), accountId, a, b);
    }

    private String applyResetPropertyOwner(Game game, GamePlayer player, SkillActivateRequest request) {
        if (request == null || request.getTargetCellId() == null) {
            throw new RuntimeException("Cần chọn ô mục tiêu (targetCellId)");
        }
        int cellId = request.getTargetCellId();
        PlayerProperty pp =
                playerPropertyRepository
                        .findByGame_GameIdAndBoardCell_CellId(game.getGameId(), cellId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy ô này"));
        GamePlayer owner = pp.getOwnerPlayer();
        BoardCell cell =
                pp.getBoardCell() != null
                        ? pp.getBoardCell()
                        : boardCellRepository.findById(cellId).orElse(null);
        String cellName = cell != null && cell.getName() != null ? cell.getName() : "Ô " + cellId;
        if (owner == null) {
            throw new RuntimeException("Ô chưa có chủ — không thể xóa quyền sở hữu");
        }
        if (Objects.equals(owner.getGamePlayerId(), player.getGamePlayerId())) {
            throw new RuntimeException("Không thể nhắm ô của chính bạn");
        }
        String oldOwnerName = displayNameForPlayer(owner);
        pp.setOwnerPlayer(null);
        pp.setHouseLevel(0);
        pp.setUpgradeSpentTotal(0L);
        playerPropertyRepository.save(pp);
        return "mục tiêu: «" + cellName + "» (chủ cũ: " + oldOwnerName + ")";
    }

    private String applyMarkAndBuyback(Skill skill, Game game, GamePlayer player, SkillActivateRequest request) {
        if (request == null || request.getTargetCellId() == null) {
            throw new RuntimeException("Cần chọn ô đất đối thủ (targetCellId)");
        }
        int cellId = request.getTargetCellId();
        PlayerProperty pp =
                playerPropertyRepository
                        .findByGame_GameIdAndBoardCell_CellId(game.getGameId(), cellId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy ô này"));
        GamePlayer owner = pp.getOwnerPlayer();
        BoardCell cell =
                pp.getBoardCell() != null
                        ? pp.getBoardCell()
                        : boardCellRepository.findById(cellId).orElse(null);
        String cellName = cell != null && cell.getName() != null ? cell.getName() : "Ô " + cellId;
        if (owner == null) {
            throw new RuntimeException("Ô chưa có chủ");
        }
        if (Objects.equals(owner.getGamePlayerId(), player.getGamePlayerId())) {
            throw new RuntimeException("Hãy chọn đất của đối thủ");
        }
        int pct = skill.getEffectValue() != null ? skill.getEffectValue() : 100;
        pct = Math.max(1, Math.min(500, pct));
        player.setSkillMarkedCellId(cellId);
        player.setSkillBuybackPercent(pct);
        gamePlayerRepository.save(player);
        return "đánh dấu «"
                + cellName
                + "» — mua lại tại "
                + pct
                + "% giá gốc khi vào ô (chủ hiện tại: "
                + displayNameForPlayer(owner)
                + ")";
    }

    private GamePlayer getCurrentTurnPlayer(Game game) {
        Integer order = game.getCurrentPlayerOrder();
        return gamePlayerRepository.findByGameIdOrderByTurnOrderAsc(game.getGameId()).stream()
                .filter(p -> Objects.equals(p.getTurnOrder(), order))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người chơi hiện tại"));
    }

    private void validateHumanTurn(GamePlayer currentTurnPlayer, Long accountId) {
        if (Boolean.TRUE.equals(currentTurnPlayer.getIsBot())) {
            throw new RuntimeException("Không phải lượt người");
        }
        if (accountId == null) {
            throw new RuntimeException("Cần đăng nhập");
        }
        Account account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(() -> new RuntimeException("Account not found"));
        UserProfile profile =
                userProfileRepository
                        .findByAccount_AccountId(account.getAccountId())
                        .orElseThrow(() -> new RuntimeException("UserProfile not found"));
        if (!Objects.equals(currentTurnPlayer.getUserProfileId(), profile.getUserProfileId())) {
            throw new RuntimeException("Không phải lượt của bạn");
        }
    }

    private String displayNameForPlayer(GamePlayer player) {
        if (Boolean.TRUE.equals(player.getIsBot())) {
            return "Bot " + (player.getTurnOrder() != null ? player.getTurnOrder() : "");
        }
        if (player.getUserProfileId() != null) {
            Optional<UserProfile> profile = userProfileRepository.findById(player.getUserProfileId());
            if (profile.isPresent()
                    && profile.get().getUsername() != null
                    && !profile.get().getUsername().isBlank()) {
                return profile.get().getUsername();
            }
        }
        return "Người chơi " + (player.getTurnOrder() != null ? player.getTurnOrder() : "");
    }
}
