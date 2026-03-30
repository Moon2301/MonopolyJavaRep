package com.game.monopoly.controller;

import com.game.monopoly.dto.HomeSummaryResponse;
import com.game.monopoly.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/summary")
    public HomeSummaryResponse getSummary(@RequestHeader(name = "X-Account-Id", required = false) Long accountId) {
        return homeService.getHomeSummary(accountId);
    }
}
