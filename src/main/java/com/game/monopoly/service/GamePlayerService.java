package com.game.monopoly.service;

import com.game.monopoly.model.inGameData.GamePlayer;
import com.game.monopoly.repository.GamePlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GamePlayerService {

    private final GamePlayerRepository gamePlayerRepository;

    public List<GamePlayer> getAllPlayers() {
        return gamePlayerRepository.findAll();
    }

    public List<GamePlayer> getPlayersByGame(Long gameId) {
        return gamePlayerRepository.findByGameId(gameId);
    }

    public GamePlayer getPlayerById(Long id) {
        return gamePlayerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }

    public GamePlayer createPlayer(GamePlayer player) {
        return gamePlayerRepository.save(player);
    }

    public GamePlayer updatePlayer(Long id, GamePlayer newPlayer) {

        GamePlayer player = getPlayerById(id);

        player.setBalance(newPlayer.getBalance());
        player.setPosition(newPlayer.getPosition());
        player.setTurnOrder(newPlayer.getTurnOrder());
        player.setIsBankrupt(newPlayer.getIsBankrupt());

        return gamePlayerRepository.save(player);
    }

    public void deletePlayer(Long id) {
        gamePlayerRepository.deleteById(id);
    }
}