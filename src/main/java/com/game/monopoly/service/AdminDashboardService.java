package com.game.monopoly.service;

import com.game.monopoly.dto.AdminDashboardResponse;
import com.game.monopoly.dto.AdminTopupLedgerItem;
import com.game.monopoly.model.enums.GameStatus;
import com.game.monopoly.repository.AccountRepository;
import com.game.monopoly.repository.CurrencyLedgerRepository;
import com.game.monopoly.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final String REASON_VNPAY = "VNPAY_TOPUP";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AccountRepository accountRepository;
    private final GameRepository gameRepository;
    private final CurrencyLedgerRepository currencyLedgerRepository;

    public AdminDashboardResponse buildDashboard() {
        long totalUsers = accountRepository.count();
        long activeGames = gameRepository.countByStatusIn(EnumSet.of(GameStatus.WAITING, GameStatus.PLAYING));

        LocalDate today = LocalDate.now(VN);
        var startOfToday = today.atStartOfDay();
        var startOfMonth = today.withDayOfMonth(1).atStartOfDay();

        long sumGoldAll = currencyLedgerRepository.sumGoldByReasonType(REASON_VNPAY);
        long sumGoldToday = currencyLedgerRepository.sumGoldByReasonTypeSince(REASON_VNPAY, startOfToday);
        long sumGoldMonth = currencyLedgerRepository.sumGoldByReasonTypeSince(REASON_VNPAY, startOfMonth);
        long txCount = currencyLedgerRepository.countByReasonType(REASON_VNPAY);

        List<AdminTopupLedgerItem> recent = currencyLedgerRepository.findRecentTopups(REASON_VNPAY, PageRequest.of(0, 15));

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .activeGames(activeGames)
                .topupTotalVnd(sumGoldAll * 100)
                .topupTodayVnd(sumGoldToday * 100)
                .topupMonthVnd(sumGoldMonth * 100)
                .topupTransactionCount(txCount)
                .recentTopups(recent)
                .build();
    }
}
