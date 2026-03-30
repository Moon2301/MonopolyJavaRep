package com.game.monopoly.service;

import com.game.monopoly.dto.LoginRequest;
import com.game.monopoly.dto.RegisterRequest;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.model.enums.UserRole;
import com.game.monopoly.model.enums.AccountStatus;
import com.game.monopoly.model.metaData.Hero;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.HeroRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final HeroRepository heroRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest request) {

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new RuntimeException("Password is required");
        }

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (userProfileRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already taken");
        }

        Account account = new Account();
        account.setEmail(request.getEmail());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setRole(UserRole.USER);
        account.setStatus(AccountStatus.ACTIVE);

        account = accountRepository.save(account);

        UserProfile profile = new UserProfile();
        profile.setAccount(account);
        profile.setUsername(request.getUsername());
        profile.setGold(1000L);
        profile.setDiamonds(10L);

        heroRepository
                .findFirstByDefaultUnlockedTrueOrderByCharacterIdAsc()
                .map(Hero::getCharacterId)
                .ifPresent(profile::setCurrentHeroId);

        userProfileRepository.save(profile);
    }
    public java.util.Map<String, Object> login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        Map<String, Object> response = new HashMap<>();
        // Bỏ JWT: trả về accountId để frontend lưu vào localStorage
        response.put("accountId", account.getAccountId());
        response.put("role", account.getRole().name()); // USER or ADMIN (giữ lại nếu UI cần)

        return response;
    }

}