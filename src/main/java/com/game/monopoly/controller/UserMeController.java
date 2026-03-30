package com.game.monopoly.controller;

import com.game.monopoly.dto.ChangeDisplayNameRequest;
import com.game.monopoly.dto.SetCurrentHeroRequest;
import com.game.monopoly.dto.ActiveGameResponse;
import com.game.monopoly.dto.UserMeSummaryResponse;
import com.game.monopoly.dto.UserMeAvatarResponse;
import com.game.monopoly.service.UserMeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
public class UserMeController {

    private final UserMeService userMeService;

    @GetMapping("/summary")
    public UserMeSummaryResponse getSummary(@RequestHeader(name = "X-Account-Id", required = false) Long accountId) {
        return userMeService.getSummary(accountId);
    }

    @GetMapping("/active-game")
    public ActiveGameResponse getActiveGame(@RequestHeader(name = "X-Account-Id", required = false) Long accountId) {
        return userMeService.getActiveGame(accountId);
    }

    @PostMapping("/display-name")
    public UserMeSummaryResponse changeDisplayName(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody(required = false) ChangeDisplayNameRequest body
    ) {
        String name = body != null ? body.getNewUsername() : null;
        return userMeService.changeDisplayName(accountId, name);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserMeAvatarResponse updateAvatar(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestParam("avatar") MultipartFile avatar
    ) {
        String avatarUrl = userMeService.updateAvatar(accountId, avatar);
        return UserMeAvatarResponse.builder().avatarUrl(avatarUrl).build();
    }

    @PostMapping("/current-hero")
    public UserMeSummaryResponse setCurrentHero(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody(required = false) SetCurrentHeroRequest body
    ) {
        Integer heroId = body != null ? body.getHeroId() : null;
        return userMeService.setCurrentHero(accountId, heroId);
    }
}

