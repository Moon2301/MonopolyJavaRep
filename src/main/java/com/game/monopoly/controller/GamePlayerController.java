package com.game.monopoly.controller;

import com.game.monopoly.model.inGameData.GamePlayer;
import com.game.monopoly.service.GamePlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/gamePlayers")
@RequiredArgsConstructor
public class GamePlayerController {

    private final GamePlayerService gamePlayerService;

    @GetMapping
    public List<GamePlayer> getAllPlayers() {
        return gamePlayerService.getAllPlayers();
    }

    @GetMapping("/game/{gameId}")
    public List<GamePlayer> getPlayersByGame(@PathVariable Long gameId) {
        return gamePlayerService.getPlayersByGame(gameId);
    }

    @PostMapping
    public GamePlayer createPlayer(@RequestBody GamePlayer player) {
        return gamePlayerService.createPlayer(player);
    }

    @PutMapping("/{id}")
    public GamePlayer updatePlayer(
            @PathVariable Long id,
            @RequestBody GamePlayer player) {
        return gamePlayerService.updatePlayer(id, player);
    }

    @DeleteMapping("/{id}")
    public void deletePlayer(@PathVariable Long id) {
        gamePlayerService.deletePlayer(id);
    }
}