package com.game.monopoly.service;

import com.game.monopoly.dto.HomeSummaryResponse;
import com.game.monopoly.model.metaData.Account;
import com.game.monopoly.model.metaData.UserProfile;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class HomeService {

    private static final String DEFAULT_AVATAR = "/images/avatar-default.png";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserProfileRepository userProfileRepository;
    private final AccountRepository accountRepository;

    public HomeSummaryResponse getHomeSummary(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        UserProfile profile = userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));

        String avatarUrl = profile.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            avatarUrl = DEFAULT_AVATAR;
        }

        return HomeSummaryResponse.builder()
                .player(HomeSummaryResponse.PlayerDto.builder()
                        .username(profile.getUsername())
                        .avatarUrl(avatarUrl)
                        .coins(profile.getGold())
                        .tickets(profile.getDiamonds())
                        .build())
                .onlineStats(HomeSummaryResponse.OnlineStatsDto.builder()
                        .playersOnline(417)
                        .build())
                .tournament(HomeSummaryResponse.TournamentDto.builder()
                        .title("Great Tournament")
                        .endsAt(LocalDateTime.now().plusHours(25).format(ISO_FORMATTER))
                        .build())
                .build();
    }
}
