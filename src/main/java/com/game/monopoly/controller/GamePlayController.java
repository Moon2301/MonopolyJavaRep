package com.game.monopoly.controller;

import com.game.monopoly.dto.*;
import com.game.monopoly.service.GamePlayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gameplay")
@RequiredArgsConstructor
public class GamePlayController {

    private final GamePlayService gamePlayService;

    @PostMapping("/bot/start")
    public StartBotGameResponse startBotGame(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody(required = false) StartBotGameRequest request
    ) {
        return gamePlayService.startBotGame(accountId, request);
    }

    @GetMapping("/{gameId}/state")
    public GameStateResponse getState(
            @PathVariable Long gameId,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return gamePlayService.getState(gameId, accountId);
    }

    @PostMapping("/{gameId}/roll")
    public GameActionResponse rollDice(
            @PathVariable Long gameId,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return gamePlayService.rollDice(gameId, accountId);
    }

    @PostMapping("/{gameId}/skill/activate")
    public GameActionResponse activateSkill(
            @PathVariable Long gameId,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody(required = false) SkillActivateRequest request
    ) {
        return gamePlayService.activateSkill(gameId, accountId, request);
    }

    @PostMapping("/{gameId}/buy")
    public GameActionResponse buyCurrentCell(
            @PathVariable Long gameId,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return gamePlayService.buyCurrentCell(gameId, accountId);
    }

    @PostMapping("/{gameId}/opponent-land")
    public GameActionResponse resolveOpponentLand(
            @PathVariable Long gameId,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody(required = false) OpponentLandResolveRequest request
    ) {
        boolean buyback = request != null && request.isBuyback();
        return gamePlayService.resolveOpponentLand(gameId, accountId, buyback);
    }

    @PostMapping("/{gameId}/upgrade")
    public GameActionResponse upgradeCurrentCell(
            @PathVariable Long gameId,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return gamePlayService.upgradeCurrentCell(gameId, accountId);
    }

    @PostMapping("/{gameId}/end-turn")
    public GameActionResponse endTurn(
            @PathVariable Long gameId,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return gamePlayService.endTurn(gameId, accountId);
    }

    @PostMapping("/{gameId}/debt/sell")
    public GameActionResponse sellAssetForDebt(
            @PathVariable Long gameId,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody DebtSellRequest request
    ) {
        return gamePlayService.sellAssetForDebt(gameId, accountId, request);
    }

    @PostMapping("/{gameId}/debt/bankrupt")
    public GameActionResponse declareBankruptcyForDebt(
            @PathVariable Long gameId,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return gamePlayService.declareBankruptcyForDebt(gameId, accountId);
    }

    @PostMapping("/{gameId}/surrender")
    public GameActionResponse surrender(
            @PathVariable Long gameId,
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId
    ) {
        return gamePlayService.surrenderGame(gameId, accountId);
    }
}
