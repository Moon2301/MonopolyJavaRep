package com.game.monopoly.service;

import com.game.monopoly.dto.HeroPurchaseResponse;
import com.game.monopoly.dto.ShopStateResponse;
import com.game.monopoly.model.enums.CurrencyType;
import com.game.monopoly.model.metaData.*;
import com.game.monopoly.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HeroShopService {

    private final UserProfileRepository userProfileRepository;
    private final HeroRepository heroRepository;
    private final UserOwnedHeroRepository userOwnedHeroRepository;
    private final CurrencyLedgerRepository currencyLedgerRepository;
    private final HeroOwnershipService heroOwnershipService;

    public ShopStateResponse getShopState(Long accountId) {
        UserProfile profile = heroOwnershipService.getProfileByAccountId(accountId);
        List<Integer> ownedHeroIds = heroOwnershipService.getOwnedHeroIds(accountId).stream().toList();

        return ShopStateResponse.builder()
                .coins(profile.getGold())
                .ownedHeroIds(ownedHeroIds)
                .build();
    }

    @Transactional
    public HeroPurchaseResponse purchaseHero(Long accountId, Integer heroId) {
        if (heroId == null) {
            throw new RuntimeException("heroId is required");
        }

        UserProfile profile = heroOwnershipService.getProfileByAccountId(accountId);
        Hero hero = heroRepository.findById(heroId)
                .orElseThrow(() -> new RuntimeException("Hero not found"));

        boolean alreadyOwned = heroOwnershipService.isHeroOwned(accountId, heroId);
        if (alreadyOwned) {
            return HeroPurchaseResponse.builder()
                    .heroId(heroId)
                    .remainingCoins(profile.getGold())
                    .purchased(false)
                    .message("Bạn đã sở hữu nhân vật này")
                    .build();
        }

        long price = hero.getPrice() == null ? 0L : hero.getPrice();
        if (profile.getGold() < price) {
            throw new RuntimeException("Không đủ xu để mua nhân vật");
        }

        long newBalance = profile.getGold() - price;
        profile.setGold(newBalance);
        userProfileRepository.save(profile);

        UserOwnedHero userOwnedHero = new UserOwnedHero();
        userOwnedHero.setUserProfile(profile);
        userOwnedHero.setHero(hero);
        userOwnedHeroRepository.save(userOwnedHero);

        CurrencyLedger ledger = new CurrencyLedger();
        ledger.setUserProfile(profile);
        ledger.setCurrencyType(CurrencyType.GOLD);
        ledger.setAmount(-price);
        ledger.setBalanceAfter(newBalance);
        ledger.setReasonType("HERO_PURCHASE");
        ledger.setReferenceId(heroId.longValue());
        currencyLedgerRepository.save(ledger);

        return HeroPurchaseResponse.builder()
                .heroId(heroId)
                .remainingCoins(newBalance)
                .purchased(true)
                .message("Mua nhân vật thành công")
                .build();
    }

}
