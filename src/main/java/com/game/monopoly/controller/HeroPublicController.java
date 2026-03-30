package com.game.monopoly.controller;

import com.game.monopoly.dto.HeroOptionResponse;
import com.game.monopoly.model.metaData.CharacterSkill;
import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.model.metaData.Skill;
import com.game.monopoly.repository.CharacterSkillRepository;
import com.game.monopoly.service.HeroService;
import com.game.monopoly.service.HeroOwnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/heroes")
@RequiredArgsConstructor
public class HeroPublicController {

    private final HeroService heroService;
    private final CharacterSkillRepository characterSkillRepository;
    private final HeroOwnershipService heroOwnershipService;

    @GetMapping
    public List<HeroOptionResponse> getHeroes() {
        return heroService.getAllHeroes().stream()
                .map(this::toHeroOption)
                .toList();
    }

    @GetMapping("/owned")
    public List<HeroOptionResponse> getOwnedHeroes(@RequestHeader(name = "X-Account-Id", required = false) Long accountId) {
        Set<Integer> ownedHeroIds = heroOwnershipService.getOwnedHeroIds(accountId);
        return heroService.getAllHeroes().stream()
                .filter(hero -> ownedHeroIds.contains(hero.getCharacterId()))
                .map(this::toHeroOption)
                .toList();
    }

    private HeroOptionResponse toHeroOption(Hero hero) {
        CharacterSkill characterSkill = characterSkillRepository.findByHero_CharacterId(hero.getCharacterId())
                .stream()
                .findFirst()
                .orElse(null);
        Skill skill = characterSkill != null ? characterSkill.getSkill() : null;

        return HeroOptionResponse.builder()
                .heroId(hero.getCharacterId())
                .name(hero.getName())
                .rarity(hero.getRarity())
                /* PNG không bắt buộc — UI dùng SVG HeroSystem, tránh 404 URL Unicode */
                .imageUrl(null)
                .price(hero.getPrice())
                .appearanceDescription(hero.getAppearanceDescription())
                .skillName(skill != null ? skill.getName() : null)
                .skillDescription(skill != null ? skill.getEffectFormula() : null)
                .skillCooldown(skill != null ? skill.getCooldown() : null)
                .defaultUnlocked(hero.getDefaultUnlocked())
                .build();
    }
}
