package com.game.monopoly.service;

import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;   // thêm dòng này

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    @Transactional
    public Account saveAccount(Account account) {

        // kiểm tra password
        if (account.getPasswordHash() == null || account.getPasswordHash().isEmpty()) {
            throw new RuntimeException("Password không được để trống");
        }

        // encode password
        String hashed = passwordEncoder.encode(account.getPasswordHash());
        account.setPasswordHash(hashed);

        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(Long id) {

        UserProfile profile = userProfileRepository.findAll().stream()
                .filter(p -> p.getAccount().getAccountId().equals(id))
                .findFirst().orElse(null);

        if (profile != null) {
            userProfileRepository.delete(profile);
        }

        accountRepository.deleteById(id);
    }

    public List<UserProfile> getAllProfiles() {
        return userProfileRepository.findAll();
    }
}