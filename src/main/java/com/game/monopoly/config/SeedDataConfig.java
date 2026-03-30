package com.game.monopoly.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.monopoly.dto.AccountSeedJson;
import com.game.monopoly.dto.HeroSeedJson;
import com.game.monopoly.model.enums.AccountStatus;
import com.game.monopoly.model.enums.UserRole;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.UserProfileRepository;
import com.game.monopoly.model.metaData.CharacterSkill;
import com.game.monopoly.model.metaData.CharacterSkillId;
import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.model.metaData.Skill;
import com.game.monopoly.repository.CharacterSkillRepository;
import com.game.monopoly.repository.HeroRepository;
import com.game.monopoly.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@RequiredArgsConstructor
public class SeedDataConfig {

    private final HeroRepository heroRepository;
    private final SkillRepository skillRepository;
    private final CharacterSkillRepository characterSkillRepository;
    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Bean
    CommandLineRunner seedHeroData() {
        return args -> {
            seedIfMissing();
            seedAccountsIfMissing();
        };
    }

    protected void seedIfMissing() {
        HeroSeedJson[] heroRows = loadHeroSeedRows();
        for (HeroSeedJson row : heroRows) {
            upsertHero(row);
        }

        upsertSkill(
                "Lộc Trời",
                "PASSIVE",
                "INCOME_PERCENT_BONUS",
                0,
                10,
                "+10% tiền nhận từ mọi nguồn"
        );
        upsertSkill(
                "Ý Chí Xác Suất",
                "ACTIVE",
                "SET_MOVE_RANGE",
                3,
                12,
                "Chọn số ô di chuyển từ 2 đến 12"
        );
        upsertSkill(
                "Tối Ưu Công Trình",
                "PASSIVE",
                "UPGRADE_COST_REDUCTION_PERCENT",
                0,
                30,
                "Giảm 30% phí nâng cấp công trình"
        );
        upsertSkill(
                "Tăng Pha",
                "ACTIVE",
                "EXTRA_RANDOM_MOVE",
                4,
                12,
                "Di chuyển thêm ngẫu nhiên 2 đến 12 ô"
        );
        upsertSkill(
                "Thẩm Vấn Rủi Ro",
                "ACTIVE",
                "DUEL_DICE",
                5,
                3,
                "Tung xúc xắc với đối thủ, <3 thì tự vào tù"
        );
        upsertSkill(
                "Xóa Quyền Sở Hữu",
                "ACTIVE",
                "RESET_PROPERTY_OWNER",
                6,
                1,
                "Biến 1 tài sản đối thủ thành vô chủ"
        );
        upsertSkill(
                "Điều Khoản Vàng",
                "ACTIVE",
                "MARK_AND_BUYBACK",
                7,
                100,
                "Đánh dấu đất đối thủ — mua lại đúng 100% giá niêm yết khi vào ô (thay vì 130%)"
        );
        upsertSkill(
                "Quỹ Khởi Đầu",
                "PASSIVE",
                "STARTING_GOLD_BONUS",
                0,
                500,
                "Nhận thêm 500 vàng khi bắt đầu ván"
        );

        for (HeroSeedJson row : heroRows) {
            if (row.getSkillName() == null || row.getSkillName().isBlank()) {
                continue;
            }
            Hero hero = heroRepository
                    .findByName(row.getName())
                    .orElseThrow(() -> new IllegalStateException("Hero sau seed không tồn tại: " + row.getName()));
            Skill skill = skillRepository
                    .findByName(row.getSkillName().trim())
                    .orElseThrow(() -> new IllegalStateException("Skill không tồn tại: " + row.getSkillName()));
            ensureCharacterSkill(hero, skill, 1);
        }
    }

    private HeroSeedJson[] loadHeroSeedRows() {
        ClassPathResource res = new ClassPathResource("seed/heroes-seed.json");
        if (!res.exists()) {
            throw new IllegalStateException("Thiếu file classpath seed/heroes-seed.json");
        }
        try (InputStream in = res.getInputStream()) {
            return objectMapper.readValue(in, HeroSeedJson[].class);
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được seed/heroes-seed.json", e);
        }
    }

    private Hero upsertHero(HeroSeedJson row) {
        if (row.getName() == null || row.getName().isBlank()) {
            throw new IllegalArgumentException("heroes-seed.json: cần trường name");
        }
        Hero hero = heroRepository.findByName(row.getName()).orElseGet(Hero::new);
        hero.setName(row.getName());
        hero.setRarity(row.getRarity() != null ? row.getRarity() : "COMMON");
        hero.setBaseHp(row.getBaseHp() != null ? row.getBaseHp() : 0);
        hero.setBaseIncomeBonus(row.getBaseIncomeBonus() != null ? row.getBaseIncomeBonus() : 0);
        hero.setPrice(row.getPrice() != null ? row.getPrice() : 0);
        hero.setAppearanceDescription(row.getAppearanceDescription());
        hero.setStartingGoldBonus(row.getStartingGoldBonus() != null ? row.getStartingGoldBonus() : 0);
        hero.setDefaultUnlocked(Boolean.TRUE.equals(row.getDefaultUnlocked()));
        hero.setIsActive(row.getIsActive() == null || row.getIsActive());
        return heroRepository.save(hero);
    }

    private Skill upsertSkill(
            String name,
            String triggerType,
            String effectType,
            int cooldown,
            Integer effectValue,
            String effectFormula
    ) {
        Skill skill = skillRepository.findByName(name).orElseGet(Skill::new);
        skill.setName(name);
        skill.setTriggerType(triggerType);
        skill.setEffectType(effectType);
        skill.setCooldown(cooldown);
        skill.setEffectValue(effectValue);
        skill.setEffectFormula(effectFormula);
        skill.setIsActive(true);
        return skillRepository.save(skill);
    }

    private void ensureCharacterSkill(Hero hero, Skill skill, Integer unlockLevel) {
        CharacterSkillId id = new CharacterSkillId(hero.getCharacterId(), skill.getSkillId());
        if (characterSkillRepository.existsById(id)) {
            return;
        }
        CharacterSkill characterSkill = new CharacterSkill(hero, skill, unlockLevel);
        characterSkillRepository.save(characterSkill);
    }

    /**
     * Seed 4 tài khoản demo (1 admin + 3 user) từ {@code seed/accounts-seed.json}.
     * Chỉ tạo khi email chưa tồn tại.
     */
    protected void seedAccountsIfMissing() {
        AccountSeedJson[] rows = loadAccountSeedRows();
        for (AccountSeedJson row : rows) {
            upsertAccountSeed(row);
        }
    }

    private AccountSeedJson[] loadAccountSeedRows() {
        ClassPathResource res = new ClassPathResource("seed/accounts-seed.json");
        if (!res.exists()) {
            return new AccountSeedJson[0];
        }
        try (InputStream in = res.getInputStream()) {
            return objectMapper.readValue(in, AccountSeedJson[].class);
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được seed/accounts-seed.json", e);
        }
    }

    private void upsertAccountSeed(AccountSeedJson row) {
        if (row.getEmail() == null || row.getEmail().isBlank()) {
            throw new IllegalArgumentException("accounts-seed.json: cần email");
        }
        if (row.getUsername() == null || row.getUsername().isBlank()) {
            throw new IllegalArgumentException("accounts-seed.json: cần username");
        }
        if (row.getPassword() == null || row.getPassword().isEmpty()) {
            throw new IllegalArgumentException("accounts-seed.json: cần password");
        }
        if (accountRepository.existsByEmail(row.getEmail().trim())) {
            return;
        }
        if (userProfileRepository.existsByUsername(row.getUsername().trim())) {
            return;
        }

        UserRole role;
        try {
            role = UserRole.valueOf(row.getRole() != null ? row.getRole().trim().toUpperCase() : "USER");
        } catch (IllegalArgumentException e) {
            role = UserRole.USER;
        }

        Account account = new Account();
        account.setEmail(row.getEmail().trim());
        account.setPasswordHash(passwordEncoder.encode(row.getPassword()));
        account.setRole(role);
        account.setStatus(AccountStatus.ACTIVE);
        account = accountRepository.save(account);

        UserProfile profile = new UserProfile();
        profile.setAccount(account);
        profile.setUsername(row.getUsername().trim());
        profile.setGold(row.getGold() != null ? row.getGold() : 1000L);
        profile.setDiamonds(row.getDiamonds() != null ? row.getDiamonds() : 10L);

        heroRepository
                .findFirstByDefaultUnlockedTrueOrderByCharacterIdAsc()
                .map(Hero::getCharacterId)
                .ifPresent(profile::setCurrentHeroId);

        userProfileRepository.save(profile);
    }
}
