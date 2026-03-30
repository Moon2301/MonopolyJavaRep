package com.game.monopoly.service;

import com.game.monopoly.model.inGameData.Game;
import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;

    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    public Game getGameById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game not found"));
    }

    public Game createGame(Game game) {
        return gameRepository.save(game);
    }

    public Game updateGame(Long id, Game newGame) {

        Game game = getGameById(id);

        game.setStatus(newGame.getStatus());
        game.setMaxPlayers(newGame.getMaxPlayers());
        game.setTurnState(newGame.getTurnState());
        game.setCurrentTurn(newGame.getCurrentTurn());
        game.setCurrentPlayerOrder(newGame.getCurrentPlayerOrder());
        game.setEndedAt(newGame.getEndedAt());
        game.setWinnerPlayerId(newGame.getWinnerPlayerId());

        if (game.getStatus() == GameStatus.FINISHED && game.getEndedAt() == null) {
            game.setEndedAt(LocalDateTime.now());
        }

        return gameRepository.save(game);
    }

    public void deleteGame(Long id) {
        gameRepository.deleteById(id);
    }
}