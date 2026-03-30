package com.game.monopoly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalUsers;
    private long activeGames;

    /** Tổng doanh thu nạp tiền (VNĐ), tính từ ledger VNPAY_TOPUP: SUM(amount) × 100 */
    private long topupTotalVnd;
    private long topupTodayVnd;
    private long topupMonthVnd;
    private long topupTransactionCount;

    @Builder.Default
    private List<AdminTopupLedgerItem> recentTopups = new ArrayList<>();
}
