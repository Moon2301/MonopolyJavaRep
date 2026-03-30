package com.game.monopoly.service;

import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.HeroRepository;
import com.game.monopoly.repository.UserOwnedHeroRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HeroOwnershipService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final HeroRepository heroRepository;
    private final UserOwnedHeroRepository userOwnedHeroRepository;

    public Set<Integer> getOwnedHeroIds(Long accountId) {
        UserProfile profile = getProfileByAccountId(accountId);
        Set<Integer> ownedHeroIds = new HashSet<>();

        heroRepository.findAll().stream()
                .filter(hero -> Boolean.TRUE.equals(hero.getDefaultUnlocked()))
                .map(Hero::getCharacterId)
                .forEach(ownedHeroIds::add);

        userOwnedHeroRepository.findByUserProfile_UserProfileId(profile.getUserProfileId()).stream()
                .map(owned -> owned.getHero().getCharacterId())
                .forEach(ownedHeroIds::add);

        return ownedHeroIds;
    }

    public boolean isHeroOwned(Long accountId, Integer heroId) {
        if (heroId == null) {
            return false;
        }
        return getOwnedHeroIds(accountId).contains(heroId);
    }

    public UserProfile getProfileByAccountId(Long accountId) {
        if (accountId == null) {
            throw new RuntimeException("Missing X-Account-Id header");
        }
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));
    }
}
