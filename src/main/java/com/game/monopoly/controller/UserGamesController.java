package com.game.monopoly.controller;

import com.game.monopoly.dto.GameHistoryItemDto;
import com.game.monopoly.service.UserGamesHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/games")
@RequiredArgsConstructor
public class UserGamesController {

    private final UserGamesHistoryService userGamesHistoryService;

    @GetMapping("/history")
    public List<GameHistoryItemDto> history(@RequestHeader(name = "X-Account-Id", required = false) Long accountId) {
        return userGamesHistoryService.getHistory(accountId);
    }
}

