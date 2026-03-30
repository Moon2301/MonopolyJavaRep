package com.game.monopoly.service;

import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestAccountService {

    private final AccountRepository accountRepository;

    public Account requireAccount(Long accountId) {
        if (accountId == null) {
            throw new RuntimeException("Missing X-Account-Id");
        }
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }
}

