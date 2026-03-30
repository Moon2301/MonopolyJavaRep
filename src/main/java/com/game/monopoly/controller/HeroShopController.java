package com.game.monopoly.controller;

import com.game.monopoly.dto.HeroPurchaseRequest;
import com.game.monopoly.dto.HeroPurchaseResponse;
import com.game.monopoly.dto.ShopStateResponse;
import com.game.monopoly.service.HeroShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/shop")
@RequiredArgsConstructor
public class HeroShopController {

    private final HeroShopService heroShopService;

    @GetMapping("/state")
    public ShopStateResponse getShopState(@RequestHeader(name = "X-Account-Id", required = false) Long accountId) {
        return heroShopService.getShopState(accountId);
    }

    @PostMapping("/purchase")
    public HeroPurchaseResponse purchaseHero(
            @RequestHeader(name = "X-Account-Id", required = false) Long accountId,
            @RequestBody HeroPurchaseRequest request
    ) {
        return heroShopService.purchaseHero(accountId, request.getHeroId());
    }
}
